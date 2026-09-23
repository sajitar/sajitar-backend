package com.sajitar.backend.application.usecase.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sajitar.backend.application.command.profile.ChangeOwnPasswordCommand;
import com.sajitar.backend.domain.exception.InvalidCredentialsException;
import com.sajitar.backend.domain.exception.ProfileNotFoundException;
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
import jakarta.validation.constraints.Size;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeOwnPasswordUseCase")
class ChangeOwnPasswordUseCaseTest {

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

    private ChangeOwnPasswordUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ChangeOwnPasswordUseCase(
                profiles,
                checkers,
                passwordHasher,
                sessions,
                attempts,
                ProfileUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Hasheia, faz wipe antes do save e apaga CHANGE_PASSWORD se existir")
    void hashesWipesThenDeletesPendingChecker() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var checker = Checker.create(existing.id(), Checker.Type.CHANGE_PASSWORD);
        credentialsReady(existing);
        when(passwordHasher.hash(ProfileUseCaseFixture.NEW_PASSWORD)).thenReturn("$2a$new");
        when(profiles.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(checkers.findByProfileIdAndType(existing.id(), Checker.Type.CHANGE_PASSWORD))
                .thenReturn(Optional.of(checker));

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
    @DisplayName("Sem checker CHANGE_PASSWORD não chama deleteById")
    void skipsDeleteWhenCheckerIsAbsent() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        credentialsReady(existing);
        when(passwordHasher.hash(ProfileUseCaseFixture.NEW_PASSWORD)).thenReturn("$2a$new");
        when(profiles.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(checkers.findByProfileIdAndType(existing.id(), Checker.Type.CHANGE_PASSWORD))
                .thenReturn(Optional.empty());

        useCase.execute(command());

        verify(sessions).wipe(existing.id());
        verify(profiles).save(any(Profile.class));
        verify(checkers, never()).deleteById(any());
    }

    @Test
    @DisplayName("Senha atual errada não grava nem encerra sessões")
    void refusesWrongCurrentPassword() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(attempts.register(AttemptScope.CREDENTIALS, ProfileUseCaseFixture.ADDRESS)).thenReturn(Optional.empty());
        when(attempts.register(AttemptScope.CREDENTIALS, existing.email())).thenReturn(Optional.empty());
        when(passwordHasher.matches(ProfileUseCaseFixture.PASSWORD, existing.password())).thenReturn(false);

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(InvalidCredentialsException.class);
        verify(sessions, never()).wipe(any());
        verify(profiles, never()).save(any());
        verify(passwordHasher, never()).hash(any());
        verify(checkers, never()).findByProfileIdAndType(any(), any());
    }

    @Test
    @DisplayName("Senhas iguais: 400 em newPassword e não consulta o repositório")
    void rejectsEqualPasswordsBeforeRepository() {
        final var command = new ChangeOwnPasswordCommand(
                ProfileUseCaseFixture.ID,
                ProfileUseCaseFixture.PASSWORD,
                ProfileUseCaseFixture.PASSWORD,
                ProfileUseCaseFixture.ADDRESS);

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getPropertyPath()).hasToString("newPassword");
        verify(profiles, never()).findById(any());
        verify(attempts, never()).register(any(), any());
        verify(sessions, never()).wipe(any());
    }

    @Test
    @DisplayName("Senha nova curta barra antes do repositório")
    void validatesPasswordBeforePorts() {
        final var command = new ChangeOwnPasswordCommand(
                ProfileUseCaseFixture.ID,
                ProfileUseCaseFixture.PASSWORD,
                "1234567",
                ProfileUseCaseFixture.ADDRESS);

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(Size.class);
        verify(profiles, never()).findById(any());
        verify(passwordHasher, never()).matches(any(), any());
    }

    @Test
    @DisplayName("Lança ProfileNotFoundException quando o perfil da sessão sumiu")
    void throwsWhenProfileDoesNotExist() {
        when(profiles.findById(ProfileUseCaseFixture.ID)).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(ProfileNotFoundException.class);
        verify(attempts, never()).register(any(), any());
        verify(sessions, never()).wipe(any());
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("Limite por endereço barra depois de carregar o perfil")
    void refusesWhenAddressLimitIsExceeded() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(attempts.register(AttemptScope.CREDENTIALS, ProfileUseCaseFixture.ADDRESS))
                .thenReturn(Optional.of(Duration.ofSeconds(12)));
        when(attempts.register(AttemptScope.CREDENTIALS, existing.email())).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(TooManyAttemptsException.class);
        assertThat(((TooManyAttemptsException) thrown).retryAfterSeconds()).isEqualTo(12L);
        verify(passwordHasher, never()).matches(any(), any());
        verify(sessions, never()).wipe(any());
    }

    @Test
    @DisplayName("Limite por e-mail barra mesmo se o endereço ainda cabe")
    void refusesWhenEmailLimitIsExceeded() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(attempts.register(AttemptScope.CREDENTIALS, ProfileUseCaseFixture.ADDRESS)).thenReturn(Optional.empty());
        when(attempts.register(AttemptScope.CREDENTIALS, existing.email()))
                .thenReturn(Optional.of(Duration.ofMillis(1500)));

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(TooManyAttemptsException.class);
        assertThat(((TooManyAttemptsException) thrown).retryAfterSeconds()).isEqualTo(2L);
        verify(sessions, never()).wipe(any());
    }

    @Test
    @DisplayName("Quando endereço e e-mail estouram, a espera é a maior das duas")
    void usesLongerWaitWhenBothLimitsAreExceeded() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(attempts.register(AttemptScope.CREDENTIALS, ProfileUseCaseFixture.ADDRESS))
                .thenReturn(Optional.of(Duration.ofSeconds(3)));
        when(attempts.register(AttemptScope.CREDENTIALS, existing.email()))
                .thenReturn(Optional.of(Duration.ofSeconds(9)));

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(TooManyAttemptsException.class);
        assertThat(((TooManyAttemptsException) thrown).retryAfterSeconds()).isEqualTo(9L);
    }

    @Test
    @DisplayName("Store de sessões fora do ar não troca a senha")
    void keepsPasswordWhenSessionStoreIsDown() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        credentialsReady(existing);
        doThrow(new SessionStoreUnavailableException()).when(sessions).wipe(existing.id());

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(SessionStoreUnavailableException.class);
        verify(profiles, never()).save(any());
        verify(passwordHasher, never()).hash(any());
        verify(checkers, never()).findByProfileIdAndType(any(), any());
    }

    @Test
    @DisplayName("Id nulo: violação @NotNull no command")
    void rejectsNullProfileId() {
        final var command = new ChangeOwnPasswordCommand(
                null,
                ProfileUseCaseFixture.PASSWORD,
                ProfileUseCaseFixture.NEW_PASSWORD,
                ProfileUseCaseFixture.ADDRESS);

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findById(any());
    }

    private void credentialsReady(final Profile existing) {
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(attempts.register(AttemptScope.CREDENTIALS, ProfileUseCaseFixture.ADDRESS)).thenReturn(Optional.empty());
        when(attempts.register(AttemptScope.CREDENTIALS, existing.email())).thenReturn(Optional.empty());
        when(passwordHasher.matches(ProfileUseCaseFixture.PASSWORD, existing.password())).thenReturn(true);
    }

    private static ChangeOwnPasswordCommand command() {
        return new ChangeOwnPasswordCommand(
                ProfileUseCaseFixture.ID,
                ProfileUseCaseFixture.PASSWORD,
                ProfileUseCaseFixture.NEW_PASSWORD,
                ProfileUseCaseFixture.ADDRESS);
    }

}
