package com.sajitar.backend.application.usecase.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sajitar.backend.application.command.profile.ChangeEmailCommand;
import com.sajitar.backend.configuration.ProfilePurgeProperties;
import com.sajitar.backend.domain.exception.EmailAlreadyRegisteredException;
import com.sajitar.backend.domain.exception.EmailNotVerifiedException;
import com.sajitar.backend.domain.exception.InvalidCheckerVerificationException;
import com.sajitar.backend.domain.exception.ProfileNotFoundException;
import com.sajitar.backend.domain.exception.SessionStoreUnavailableException;
import com.sajitar.backend.domain.exception.TooManyAttemptsException;
import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.model.profile.Profile;
import com.sajitar.backend.domain.model.token.AttemptScope;
import com.sajitar.backend.domain.port.checker.CheckerRepository;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.port.token.AttemptLimiter;
import com.sajitar.backend.domain.port.token.SessionStore;

import jakarta.validation.ConstraintViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeEmailUseCase")
class ChangeEmailUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private static final String CODE = "654321";

    @Mock
    private ProfileRepository profiles;

    @Mock
    private CheckerRepository checkers;

    @Mock
    private SessionStore sessions;

    @Mock
    private AttemptLimiter attempts;

    private ChangeEmailUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ChangeEmailUseCase(
                profiles,
                checkers,
                sessions,
                attempts,
                CLOCK,
                new ProfilePurgeProperties(30, 30, 30, 30, 30, "UTC"),
                ProfileUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Encerra sessões, grava o payload e apaga o checker")
    void wipesSavesThenDeletesChecker() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        final var checker = currentChangeEmail(profile.id());
        ready(profile);
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.CHANGE_EMAIL)).thenReturn(Optional.of(checker));
        when(profiles.findByEmail(ProfileUseCaseFixture.NEW_EMAIL)).thenReturn(Optional.empty());
        when(profiles.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(command());

        final var captor = ArgumentCaptor.forClass(Profile.class);
        verify(profiles).save(captor.capture());
        assertThat(captor.getValue().email()).isEqualTo(ProfileUseCaseFixture.NEW_EMAIL);
        assertThat(captor.getValue().id()).isEqualTo(profile.id());
        assertThat(captor.getValue().password()).isEqualTo(profile.password());
        final var order = inOrder(sessions, profiles, checkers);
        order.verify(sessions).wipe(profile.id());
        order.verify(profiles).save(any(Profile.class));
        order.verify(checkers).deleteById(checker.id());
    }

    @Test
    @DisplayName("Perfil ausente responde 404")
    void missingProfileReturns404() {
        when(profiles.findById(ProfileUseCaseFixture.ID)).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(ProfileNotFoundException.class);
        verify(attempts, never()).register(any(), any());
        verify(sessions, never()).wipe(any());
    }

    @Test
    @DisplayName("VERIFY_EMAIL responde 403 sem wipe")
    void unverifiedEmailDoesNotWipe() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        ready(profile);
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL))
                .thenReturn(Optional.of(Checker.create(profile.id(), Checker.Type.VERIFY_EMAIL)));

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(EmailNotVerifiedException.class);
        verify(sessions, never()).wipe(any());
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("Checker ausente responde 401 em code")
    void missingCheckerReturnsInvalidCode() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        ready(profile);
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.CHANGE_EMAIL)).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(InvalidCheckerVerificationException.class);
        verify(sessions, never()).wipe(any());
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("Payload nulo responde 401 sem wipe")
    void nullPayloadReturnsInvalidCode() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        final var checker = new Checker(
                Checker.uuidV7At(NOW.minus(Duration.ofMinutes(1))),
                profile.id(),
                Checker.Type.CHANGE_EMAIL,
                CODE,
                null);
        ready(profile);
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.CHANGE_EMAIL)).thenReturn(Optional.of(checker));

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(InvalidCheckerVerificationException.class);
        verify(sessions, never()).wipe(any());
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("Checker vencido responde 401 em code")
    void expiredCheckerReturnsInvalidCode() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        final var expired = new Checker(
                Checker.uuidV7At(NOW.minus(Duration.ofMinutes(31))),
                profile.id(),
                Checker.Type.CHANGE_EMAIL,
                CODE,
                ProfileUseCaseFixture.NEW_EMAIL);
        ready(profile);
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.CHANGE_EMAIL)).thenReturn(Optional.of(expired));

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(InvalidCheckerVerificationException.class);
        verify(sessions, never()).wipe(any());
    }

    @Test
    @DisplayName("Código divergente responde 401 e não altera o checker")
    void mismatchedCodeDoesNotChangeChecker() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        final var checker = currentChangeEmail(profile.id());
        ready(profile);
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.CHANGE_EMAIL)).thenReturn(Optional.of(checker));

        final var thrown = catchThrowable(() -> useCase.execute(new ChangeEmailCommand(
                profile.id(),
                "123456",
                ProfileUseCaseFixture.ADDRESS)));

        assertThat(thrown).isInstanceOf(InvalidCheckerVerificationException.class);
        verify(sessions, never()).wipe(any());
        verify(profiles, never()).save(any());
        verify(checkers, never()).deleteById(any());
    }

    @Test
    @DisplayName("E-mail de outro perfil responde 409 com o checker preservado")
    void takenEmailReturns409WithoutWipe() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        final var checker = currentChangeEmail(profile.id());
        final var other = profile.withId(UUID.randomUUID()).withEmail(ProfileUseCaseFixture.NEW_EMAIL);
        ready(profile);
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.CHANGE_EMAIL)).thenReturn(Optional.of(checker));
        when(profiles.findByEmail(ProfileUseCaseFixture.NEW_EMAIL)).thenReturn(Optional.of(other));

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(EmailAlreadyRegisteredException.class);
        verify(sessions, never()).wipe(any());
        verify(profiles, never()).save(any());
        verify(checkers, never()).deleteById(any());
    }

    @Test
    @DisplayName("Código mal formado barra antes do repositório")
    void validatesCodeBeforePorts() {
        final var thrown = catchThrowable(() -> useCase.execute(new ChangeEmailCommand(
                ProfileUseCaseFixture.ID,
                "12a456",
                ProfileUseCaseFixture.ADDRESS)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findById(any());
        verify(attempts, never()).register(any(), any());
    }

    @Test
    @DisplayName("Limite por endereço barra depois de achar o perfil")
    void refusesWhenAddressLimitIsExceeded() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        when(profiles.findById(profile.id())).thenReturn(Optional.of(profile));
        when(attempts.register(AttemptScope.CREDENTIALS, ProfileUseCaseFixture.ADDRESS))
                .thenReturn(Optional.of(Duration.ofSeconds(12)));
        when(attempts.register(AttemptScope.CREDENTIALS, profile.email())).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(TooManyAttemptsException.class);
        verify(sessions, never()).wipe(any());
    }

    @Test
    @DisplayName("Limite por e-mail barra mesmo se o endereço ainda cabe")
    void refusesWhenEmailLimitIsExceeded() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        when(profiles.findById(profile.id())).thenReturn(Optional.of(profile));
        when(attempts.register(AttemptScope.CREDENTIALS, ProfileUseCaseFixture.ADDRESS)).thenReturn(Optional.empty());
        when(attempts.register(AttemptScope.CREDENTIALS, profile.email()))
                .thenReturn(Optional.of(Duration.ofSeconds(9)));

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(TooManyAttemptsException.class);
        verify(sessions, never()).wipe(any());
    }

    @Test
    @DisplayName("Store de sessões fora do ar não troca o e-mail")
    void keepsEmailWhenSessionStoreIsDown() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        final var checker = currentChangeEmail(profile.id());
        ready(profile);
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.CHANGE_EMAIL)).thenReturn(Optional.of(checker));
        when(profiles.findByEmail(ProfileUseCaseFixture.NEW_EMAIL)).thenReturn(Optional.empty());
        doThrow(new SessionStoreUnavailableException()).when(sessions).wipe(profile.id());

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(SessionStoreUnavailableException.class);
        verify(profiles, never()).save(any());
        verify(checkers, never()).deleteById(any());
    }

    private void ready(final Profile profile) {
        when(profiles.findById(profile.id())).thenReturn(Optional.of(profile));
        when(attempts.register(AttemptScope.CREDENTIALS, ProfileUseCaseFixture.ADDRESS)).thenReturn(Optional.empty());
        when(attempts.register(AttemptScope.CREDENTIALS, profile.email())).thenReturn(Optional.empty());
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL)).thenReturn(Optional.empty());
    }

    private static Checker currentChangeEmail(final UUID profileId) {
        return new Checker(
                Checker.uuidV7At(NOW.minus(Duration.ofMinutes(1))),
                profileId,
                Checker.Type.CHANGE_EMAIL,
                CODE,
                ProfileUseCaseFixture.NEW_EMAIL);
    }

    private static ChangeEmailCommand command() {
        return new ChangeEmailCommand(ProfileUseCaseFixture.ID, CODE, ProfileUseCaseFixture.ADDRESS);
    }

}
