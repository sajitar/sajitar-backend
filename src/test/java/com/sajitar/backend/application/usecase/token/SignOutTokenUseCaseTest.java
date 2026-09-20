package com.sajitar.backend.application.usecase.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sajitar.backend.application.command.token.SignOutTokenCommand;
import com.sajitar.backend.domain.exception.InvalidCredentialsException;
import com.sajitar.backend.domain.exception.SessionNotFoundException;
import com.sajitar.backend.domain.exception.TooManyAttemptsException;
import com.sajitar.backend.domain.model.token.AttemptScope;
import com.sajitar.backend.domain.port.PasswordHasher;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.port.token.AttemptLimiter;
import com.sajitar.backend.domain.port.token.SessionStore;

import jakarta.validation.ConstraintViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("SignOutTokenUseCase")
class SignOutTokenUseCaseTest {

    private static final UUID CURRENT_SESSION_ID = UUID.fromString("018f3c2a-7b00-7c3d-9e1a-000000000010");

    private static final UUID OTHER_SESSION_ID = UUID.fromString("018f3c2a-7b00-7c3d-9e1a-000000000020");

    @Mock
    private ProfileRepository profiles;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private SessionStore sessions;

    @Mock
    private AttemptLimiter attempts;

    private SignOutTokenUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SignOutTokenUseCase(profiles, passwordHasher, sessions, attempts, TokenUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Sair só da sessão corrente dispensa a senha")
    void closesCurrentSessionWithoutPassword() {
        final var command = command(List.of(CURRENT_SESSION_ID), null);
        when(sessions.close(TokenUseCaseFixture.PROFILE_ID, command.ids())).thenReturn(true);

        assertThatCode(() -> useCase.execute(command)).doesNotThrowAnyException();

        verify(profiles, never()).findById(any());
        verify(passwordHasher, never()).matches(any(), any());
        verify(attempts, never()).register(any(), any());
    }

    @Test
    @DisplayName("Id da sessão corrente repetido continua dispensando a senha")
    void closesDuplicatedCurrentSessionWithoutPassword() {
        final var command = command(List.of(CURRENT_SESSION_ID, CURRENT_SESSION_ID), null);
        when(sessions.close(TokenUseCaseFixture.PROFILE_ID, command.ids())).thenReturn(true);

        assertThatCode(() -> useCase.execute(command)).doesNotThrowAnyException();

        verify(passwordHasher, never()).matches(any(), any());
        verify(attempts, never()).register(any(), any());
    }

    @Test
    @DisplayName("Sair de outra sessão encerra o lote quando a senha confere")
    void closesOtherSessionWithPassword() {
        final var command = command(List.of(CURRENT_SESSION_ID, OTHER_SESSION_ID), TokenUseCaseFixture.PASSWORD);
        final var profile = TokenUseCaseFixture.persistedProfile();
        when(profiles.findById(TokenUseCaseFixture.PROFILE_ID)).thenReturn(Optional.of(profile));
        when(passwordHasher.matches(TokenUseCaseFixture.PASSWORD, profile.password())).thenReturn(true);
        when(sessions.close(TokenUseCaseFixture.PROFILE_ID, command.ids())).thenReturn(true);

        assertThatCode(() -> useCase.execute(command)).doesNotThrowAnyException();

        verify(sessions).close(TokenUseCaseFixture.PROFILE_ID, command.ids());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "", "   ", "1234567" })
    @DisplayName("Senha exigida ausente ou mal formada barra antes do store")
    void requiresWellFormedPasswordForOtherSessions(final String password) {
        final var command = command(List.of(OTHER_SESSION_ID), password);

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getPropertyPath()).hasToString("password");
        verify(attempts).register(AttemptScope.CREDENTIALS, TokenUseCaseFixture.ADDRESS);
        verify(profiles, never()).findById(any());
        verify(sessions, never()).close(any(), any());
    }

    @Test
    @DisplayName("Senha que não confere não revela se a sessão alheia existe")
    void refusesWrongPassword() {
        final var command = command(List.of(OTHER_SESSION_ID), TokenUseCaseFixture.PASSWORD);
        final var profile = TokenUseCaseFixture.persistedProfile();
        when(profiles.findById(TokenUseCaseFixture.PROFILE_ID)).thenReturn(Optional.of(profile));
        when(passwordHasher.matches(TokenUseCaseFixture.PASSWORD, profile.password())).thenReturn(false);

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(InvalidCredentialsException.class);
        assertThat(((InvalidCredentialsException) thrown).content().get("credentials"))
                .containsExactly(InvalidCredentialsException.MESSAGE_KEY);
        verify(sessions, never()).close(any(), any());
    }

    @Test
    @DisplayName("Perfil sumido no banco cai em credenciais inválidas")
    void refusesVanishedProfile() {
        final var command = command(List.of(OTHER_SESSION_ID), TokenUseCaseFixture.PASSWORD);
        when(profiles.findById(TokenUseCaseFixture.PROFILE_ID)).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(InvalidCredentialsException.class);
        verify(passwordHasher, never()).matches(any(), any());
        verify(sessions, never()).close(any(), any());
    }

    @Test
    @DisplayName("Id que não é sessão ativa do perfil responde 404 sem encerrar nada")
    void refusesUnknownSession() {
        final var command = command(List.of(CURRENT_SESSION_ID), null);
        when(sessions.close(TokenUseCaseFixture.PROFILE_ID, command.ids())).thenReturn(false);

        assertThat(catchThrowable(() -> useCase.execute(command))).isInstanceOf(SessionNotFoundException.class);
    }

    @Test
    @DisplayName("Lista de sessões vazia barra antes do store")
    void validatesEmptyIdsBeforeStore() {
        final var thrown = catchThrowable(() -> useCase.execute(command(List.of(), null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getPropertyPath()).hasToString("ids");
        verify(attempts, never()).register(any(), any());
        verify(sessions, never()).close(any(), any());
    }

    @Test
    @DisplayName("Lista de sessões ausente barra antes do store")
    void validatesAbsentIdsBeforeStore() {
        final var thrown = catchThrowable(() -> useCase.execute(command(null, null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(attempts, never()).register(any(), any());
        verify(sessions, never()).close(any(), any());
    }

    @Test
    @DisplayName("Id nulo na lista barra antes do store")
    void validatesNullIdBeforeStore() {
        final var thrown = catchThrowable(() -> useCase.execute(command(Arrays.asList(CURRENT_SESSION_ID, null), null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(attempts, never()).register(any(), any());
        verify(sessions, never()).close(any(), any());
    }

    @Test
    @DisplayName("Limite por endereço barra antes de conferir a senha")
    void refusesWhenAddressLimitIsExceeded() {
        when(attempts.register(AttemptScope.CREDENTIALS, TokenUseCaseFixture.ADDRESS))
                .thenReturn(Optional.of(Duration.ofSeconds(20)));

        final var thrown = catchThrowable(
                () -> useCase.execute(command(List.of(OTHER_SESSION_ID), TokenUseCaseFixture.PASSWORD)));

        assertThat(thrown).isInstanceOf(TooManyAttemptsException.class);
        assertThat(((TooManyAttemptsException) thrown).retryAfterSeconds()).isEqualTo(20L);
        verify(profiles, never()).findById(any());
        verify(sessions, never()).close(any(), any());
    }

    private static SignOutTokenCommand command(final List<UUID> ids, final String password) {
        return new SignOutTokenCommand(
                TokenUseCaseFixture.PROFILE_ID, CURRENT_SESSION_ID, ids, password, TokenUseCaseFixture.ADDRESS);
    }

}
