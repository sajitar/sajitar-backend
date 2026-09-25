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

import com.sajitar.backend.application.command.profile.ConfirmPasswordRecoveryCommand;
import com.sajitar.backend.configuration.ProfilePurgeProperties;
import com.sajitar.backend.domain.exception.InvalidCheckerVerificationException;
import com.sajitar.backend.domain.exception.SessionStoreUnavailableException;
import com.sajitar.backend.domain.exception.TooManyAttemptsException;
import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.model.profile.Profile;
import com.sajitar.backend.domain.model.token.AttemptScope;
import com.sajitar.backend.domain.port.PasswordHasher;
import com.sajitar.backend.domain.port.checker.CheckerRepository;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.port.token.AttemptLimiter;
import com.sajitar.backend.domain.port.token.SessionStore;

import jakarta.validation.ConstraintViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConfirmPasswordRecoveryUseCase")
class ConfirmPasswordRecoveryUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private static final String CODE = "123456";

    @Mock
    private ProfileRepository profiles;

    @Mock
    private CheckerRepository checkers;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private SessionStore sessions;

    @Mock
    private AttemptLimiter attempts;

    private ConfirmPasswordRecoveryUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ConfirmPasswordRecoveryUseCase(
                profiles,
                checkers,
                passwordHasher,
                sessions,
                attempts,
                CLOCK,
                new ProfilePurgeProperties(48, 12, "UTC"),
                ProfileUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Hasheia, encerra sessões antes do save e apaga o checker")
    void hashesWipesThenDeletesChecker() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var checker = currentChangePassword(existing.id());
        limitsAccepted(existing.email());
        when(profiles.findByEmail(existing.email())).thenReturn(Optional.of(existing));
        when(checkers.findByProfileIdAndType(existing.id(), Checker.Type.CHANGE_PASSWORD))
                .thenReturn(Optional.of(checker));
        when(passwordHasher.hash(ProfileUseCaseFixture.NEW_PASSWORD)).thenReturn("$2a$new");
        when(profiles.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(command());

        final var captor = ArgumentCaptor.forClass(Profile.class);
        verify(profiles).save(captor.capture());
        assertThat(captor.getValue().password()).isEqualTo("$2a$new");
        assertThat(captor.getValue().id()).isEqualTo(existing.id());
        final var order = inOrder(sessions, passwordHasher, profiles, checkers);
        order.verify(sessions).wipe(existing.id());
        order.verify(passwordHasher).hash(ProfileUseCaseFixture.NEW_PASSWORD);
        order.verify(profiles).save(any(Profile.class));
        order.verify(checkers).deleteById(checker.id());
    }

    @Test
    @DisplayName("E-mail inexistente responde 401 em code")
    void unknownEmailReturnsInvalidCode() {
        limitsAccepted(ProfileUseCaseFixture.EMAIL);
        when(profiles.findByEmail(ProfileUseCaseFixture.EMAIL)).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(InvalidCheckerVerificationException.class);
        verify(checkers, never()).findByProfileIdAndType(any(), any());
        verify(sessions, never()).wipe(any());
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("Checker ausente responde 401 em code")
    void missingCheckerReturnsInvalidCode() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        limitsAccepted(existing.email());
        when(profiles.findByEmail(existing.email())).thenReturn(Optional.of(existing));
        when(checkers.findByProfileIdAndType(existing.id(), Checker.Type.CHANGE_PASSWORD))
                .thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(InvalidCheckerVerificationException.class);
        verify(sessions, never()).wipe(any());
        verify(profiles, never()).save(any());
        verify(checkers, never()).deleteById(any());
    }

    @Test
    @DisplayName("Checker vencido responde 401 em code e não apaga")
    void expiredCheckerReturnsInvalidCode() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var expired = new Checker(
                Checker.uuidV7At(NOW.minus(Duration.ofHours(13))),
                existing.id(),
                Checker.Type.CHANGE_PASSWORD,
                CODE,
                null);
        limitsAccepted(existing.email());
        when(profiles.findByEmail(existing.email())).thenReturn(Optional.of(existing));
        when(checkers.findByProfileIdAndType(existing.id(), Checker.Type.CHANGE_PASSWORD))
                .thenReturn(Optional.of(expired));

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(InvalidCheckerVerificationException.class);
        verify(sessions, never()).wipe(any());
        verify(profiles, never()).save(any());
        verify(checkers, never()).deleteById(any());
    }

    @Test
    @DisplayName("Código divergente responde 401 e não altera o checker")
    void mismatchedCodeDoesNotChangeChecker() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var checker = currentChangePassword(existing.id());
        limitsAccepted(existing.email());
        when(profiles.findByEmail(existing.email())).thenReturn(Optional.of(existing));
        when(checkers.findByProfileIdAndType(existing.id(), Checker.Type.CHANGE_PASSWORD))
                .thenReturn(Optional.of(checker));

        final var thrown = catchThrowable(() -> useCase.execute(new ConfirmPasswordRecoveryCommand(
                existing.email(),
                "654321",
                ProfileUseCaseFixture.NEW_PASSWORD,
                ProfileUseCaseFixture.ADDRESS)));

        assertThat(thrown).isInstanceOf(InvalidCheckerVerificationException.class);
        verify(sessions, never()).wipe(any());
        verify(profiles, never()).save(any());
        verify(checkers, never()).save(any());
        verify(checkers, never()).deleteById(any());
    }

    @Test
    @DisplayName("Código mal formado barra antes do repositório")
    void validatesCodeBeforePorts() {
        final var thrown = catchThrowable(() -> useCase.execute(new ConfirmPasswordRecoveryCommand(
                ProfileUseCaseFixture.EMAIL,
                "12a456",
                ProfileUseCaseFixture.NEW_PASSWORD,
                ProfileUseCaseFixture.ADDRESS)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(attempts, never()).register(any(), any());
        verify(profiles, never()).findByEmail(any());
    }

    @Test
    @DisplayName("Senha nova curta barra antes do repositório")
    void validatesPasswordBeforePorts() {
        final var thrown = catchThrowable(() -> useCase.execute(new ConfirmPasswordRecoveryCommand(
                ProfileUseCaseFixture.EMAIL,
                CODE,
                "1234567",
                ProfileUseCaseFixture.ADDRESS)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(attempts, never()).register(any(), any());
        verify(profiles, never()).findByEmail(any());
    }

    @Test
    @DisplayName("E-mail inválido barra antes do repositório")
    void validatesEmailBeforePorts() {
        final var thrown = catchThrowable(() -> useCase.execute(new ConfirmPasswordRecoveryCommand(
                "not-an-email",
                CODE,
                ProfileUseCaseFixture.NEW_PASSWORD,
                ProfileUseCaseFixture.ADDRESS)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(attempts, never()).register(any(), any());
        verify(profiles, never()).findByEmail(any());
    }

    @Test
    @DisplayName("Limite por endereço barra antes de consultar o perfil")
    void refusesWhenAddressLimitIsExceeded() {
        when(attempts.register(AttemptScope.CREDENTIALS, ProfileUseCaseFixture.ADDRESS))
                .thenReturn(Optional.of(Duration.ofSeconds(12)));
        when(attempts.register(AttemptScope.CREDENTIALS, ProfileUseCaseFixture.EMAIL)).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(TooManyAttemptsException.class);
        assertThat(((TooManyAttemptsException) thrown).retryAfterSeconds()).isEqualTo(12L);
        verify(profiles, never()).findByEmail(any());
        verify(sessions, never()).wipe(any());
    }

    @Test
    @DisplayName("Limite por e-mail barra mesmo se o endereço ainda cabe")
    void refusesWhenEmailLimitIsExceeded() {
        when(attempts.register(AttemptScope.CREDENTIALS, ProfileUseCaseFixture.ADDRESS)).thenReturn(Optional.empty());
        when(attempts.register(AttemptScope.CREDENTIALS, ProfileUseCaseFixture.EMAIL))
                .thenReturn(Optional.of(Duration.ofSeconds(9)));

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(TooManyAttemptsException.class);
        assertThat(((TooManyAttemptsException) thrown).retryAfterSeconds()).isEqualTo(9L);
        verify(profiles, never()).findByEmail(any());
    }

    @Test
    @DisplayName("Quando endereço e e-mail estouram, a espera é a maior das duas")
    void usesLongerWaitWhenBothLimitsAreExceeded() {
        when(attempts.register(AttemptScope.CREDENTIALS, ProfileUseCaseFixture.ADDRESS))
                .thenReturn(Optional.of(Duration.ofSeconds(3)));
        when(attempts.register(AttemptScope.CREDENTIALS, ProfileUseCaseFixture.EMAIL))
                .thenReturn(Optional.of(Duration.ofSeconds(9)));

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(TooManyAttemptsException.class);
        assertThat(((TooManyAttemptsException) thrown).retryAfterSeconds()).isEqualTo(9L);
    }

    @Test
    @DisplayName("Store de sessões fora do ar não troca a senha")
    void keepsPasswordWhenSessionStoreIsDown() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var checker = currentChangePassword(existing.id());
        limitsAccepted(existing.email());
        when(profiles.findByEmail(existing.email())).thenReturn(Optional.of(existing));
        when(checkers.findByProfileIdAndType(existing.id(), Checker.Type.CHANGE_PASSWORD))
                .thenReturn(Optional.of(checker));
        doThrow(new SessionStoreUnavailableException()).when(sessions).wipe(existing.id());

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(SessionStoreUnavailableException.class);
        verify(profiles, never()).save(any());
        verify(passwordHasher, never()).hash(any());
        verify(checkers, never()).deleteById(any());
    }

    private void limitsAccepted(final String email) {
        when(attempts.register(AttemptScope.CREDENTIALS, ProfileUseCaseFixture.ADDRESS)).thenReturn(Optional.empty());
        when(attempts.register(AttemptScope.CREDENTIALS, email)).thenReturn(Optional.empty());
    }

    private static Checker currentChangePassword(final UUID profileId) {
        return new Checker(
                Checker.uuidV7At(NOW.minus(Duration.ofHours(1))),
                profileId,
                Checker.Type.CHANGE_PASSWORD,
                CODE,
                null);
    }

    private static ConfirmPasswordRecoveryCommand command() {
        return new ConfirmPasswordRecoveryCommand(
                ProfileUseCaseFixture.EMAIL,
                CODE,
                ProfileUseCaseFixture.NEW_PASSWORD,
                ProfileUseCaseFixture.ADDRESS);
    }

}
