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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sajitar.backend.application.command.profile.DeleteProfileCommand;
import com.sajitar.backend.configuration.ProfilePurgeProperties;
import com.sajitar.backend.domain.exception.EmailNotVerifiedException;
import com.sajitar.backend.domain.exception.ForbiddenProfileDeletionException;
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
import jakarta.validation.constraints.NotNull;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteProfileUseCase")
class DeleteProfileUseCaseTest {

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

    private DeleteProfileUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteProfileUseCase(
                profiles,
                checkers,
                sessions,
                attempts,
                CLOCK,
                new ProfilePurgeProperties(30, 30, 30, 30, "UTC"),
                ProfileUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Encerra sessões e exclui o perfil sem apagar o checker")
    void wipesThenDeletesWithoutDeletingChecker() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        ready(profile);
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.DELETE_PROFILE))
                .thenReturn(Optional.of(currentDeleteProfile(profile.id())));

        useCase.execute(command());

        final var order = inOrder(sessions, profiles);
        order.verify(sessions).wipe(profile.id());
        order.verify(profiles).deleteById(profile.id());
        verify(checkers, never()).deleteById(any());
    }

    @Test
    @DisplayName("Id diferente do caller responde 403 sem repositório")
    void foreignIdIsForbiddenBeforeRepository() {
        final var thrown = catchThrowable(() -> useCase.execute(new DeleteProfileCommand(
                ProfileUseCaseFixture.VIEWER_ID,
                ProfileUseCaseFixture.ID,
                CODE,
                ProfileUseCaseFixture.ADDRESS)));

        assertThat(thrown).isInstanceOf(ForbiddenProfileDeletionException.class);
        assertThat(((ForbiddenProfileDeletionException) thrown).content().get("id"))
                .containsExactly(ForbiddenProfileDeletionException.MESSAGE_KEY);
        verify(profiles, never()).findById(any());
        verify(attempts, never()).register(any(), any());
        verify(sessions, never()).wipe(any());
    }

    @Test
    @DisplayName("Caller MASTER também não exclui perfil alheio")
    void masterCannotDeleteAnotherProfile() {
        final var thrown = catchThrowable(() -> useCase.execute(new DeleteProfileCommand(
                ProfileUseCaseFixture.VIEWER_ID,
                ProfileUseCaseFixture.ID,
                CODE,
                ProfileUseCaseFixture.ADDRESS)));

        assertThat(thrown).isInstanceOf(ForbiddenProfileDeletionException.class);
        verify(profiles, never()).findById(any());
    }

    @Test
    @DisplayName("Perfil ausente responde 404")
    void missingOwnProfileReturns404() {
        when(profiles.findById(ProfileUseCaseFixture.ID)).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(ProfileNotFoundException.class);
        verify(attempts, never()).register(any(), any());
        verify(sessions, never()).wipe(any());
        verify(profiles, never()).deleteById(any());
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
        verify(checkers, never()).findByProfileIdAndType(profile.id(), Checker.Type.DELETE_PROFILE);
        verify(sessions, never()).wipe(any());
        verify(profiles, never()).deleteById(any());
    }

    @Test
    @DisplayName("Checker ausente responde 401 em code")
    void missingCheckerReturnsInvalidCode() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        ready(profile);
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.DELETE_PROFILE)).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(InvalidCheckerVerificationException.class);
        verify(sessions, never()).wipe(any());
        verify(profiles, never()).deleteById(any());
    }

    @Test
    @DisplayName("Checker vencido responde 401 em code")
    void expiredCheckerReturnsInvalidCode() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        final var expired = new Checker(
                Checker.uuidV7At(NOW.minus(Duration.ofMinutes(31))),
                profile.id(),
                Checker.Type.DELETE_PROFILE,
                CODE,
                null);
        ready(profile);
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.DELETE_PROFILE)).thenReturn(Optional.of(expired));

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(InvalidCheckerVerificationException.class);
        verify(sessions, never()).wipe(any());
        verify(profiles, never()).deleteById(any());
    }

    @Test
    @DisplayName("Código divergente responde 401 e não altera o checker")
    void mismatchedCodeDoesNotChangeChecker() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        ready(profile);
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.DELETE_PROFILE))
                .thenReturn(Optional.of(currentDeleteProfile(profile.id())));

        final var thrown = catchThrowable(() -> useCase.execute(new DeleteProfileCommand(
                profile.id(),
                profile.id(),
                "123456",
                ProfileUseCaseFixture.ADDRESS)));

        assertThat(thrown).isInstanceOf(InvalidCheckerVerificationException.class);
        verify(sessions, never()).wipe(any());
        verify(profiles, never()).deleteById(any());
        verify(checkers, never()).deleteById(any());
        verify(checkers, never()).save(any());
    }

    @Test
    @DisplayName("Código mal formado barra antes do repositório")
    void validatesCodeBeforePorts() {
        final var thrown = catchThrowable(() -> useCase.execute(new DeleteProfileCommand(
                ProfileUseCaseFixture.ID,
                ProfileUseCaseFixture.ID,
                "12a456",
                ProfileUseCaseFixture.ADDRESS)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findById(any());
        verify(attempts, never()).register(any(), any());
    }

    @Test
    @DisplayName("Id nulo: não chama o repositório")
    void doesNotCallRepositoryWhenIdIsNull() {
        final var thrown = catchThrowable(() -> useCase.execute(new DeleteProfileCommand(
                null,
                ProfileUseCaseFixture.ID,
                CODE,
                ProfileUseCaseFixture.ADDRESS)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotNull.class);
        verify(profiles, never()).findById(any(UUID.class));
        verify(sessions, never()).wipe(any());
        verify(profiles, never()).deleteById(any());
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
        verify(profiles, never()).deleteById(any());
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
    @DisplayName("Quando endereço e e-mail estouram, a espera é a maior das duas")
    void usesLongerWaitWhenBothLimitsAreExceeded() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        when(profiles.findById(profile.id())).thenReturn(Optional.of(profile));
        when(attempts.register(AttemptScope.CREDENTIALS, ProfileUseCaseFixture.ADDRESS))
                .thenReturn(Optional.of(Duration.ofSeconds(3)));
        when(attempts.register(AttemptScope.CREDENTIALS, profile.email()))
                .thenReturn(Optional.of(Duration.ofSeconds(9)));

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(TooManyAttemptsException.class);
        assertThat(((TooManyAttemptsException) thrown).retryAfterSeconds()).isEqualTo(9L);
        verify(sessions, never()).wipe(any());
    }

    @Test
    @DisplayName("Store de sessões fora do ar não exclui o perfil")
    void keepsProfileWhenSessionStoreIsDown() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        ready(profile);
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.DELETE_PROFILE))
                .thenReturn(Optional.of(currentDeleteProfile(profile.id())));
        doThrow(new SessionStoreUnavailableException()).when(sessions).wipe(profile.id());

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(SessionStoreUnavailableException.class);
        verify(profiles, never()).deleteById(any());
        verify(checkers, never()).deleteById(any());
    }

    private void ready(final Profile profile) {
        when(profiles.findById(profile.id())).thenReturn(Optional.of(profile));
        when(attempts.register(AttemptScope.CREDENTIALS, ProfileUseCaseFixture.ADDRESS)).thenReturn(Optional.empty());
        when(attempts.register(AttemptScope.CREDENTIALS, profile.email())).thenReturn(Optional.empty());
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL)).thenReturn(Optional.empty());
    }

    private static Checker currentDeleteProfile(final UUID profileId) {
        return new Checker(
                Checker.uuidV7At(NOW.minus(Duration.ofMinutes(1))),
                profileId,
                Checker.Type.DELETE_PROFILE,
                CODE,
                null);
    }

    private static DeleteProfileCommand command() {
        return new DeleteProfileCommand(
                ProfileUseCaseFixture.ID,
                ProfileUseCaseFixture.ID,
                CODE,
                ProfileUseCaseFixture.ADDRESS);
    }

}
