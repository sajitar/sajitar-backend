package com.sajitar.backend.application.usecase.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sajitar.backend.application.command.token.SignInTokenCommand;
import com.sajitar.backend.domain.exception.EmailNotVerifiedException;
import com.sajitar.backend.domain.exception.InvalidCredentialsException;
import com.sajitar.backend.domain.exception.TooManyAttemptsException;
import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.model.token.AttemptScope;
import com.sajitar.backend.domain.model.token.Session;
import com.sajitar.backend.domain.port.PasswordHasher;
import com.sajitar.backend.domain.port.checker.CheckerRepository;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.port.token.AttemptLimiter;
import com.sajitar.backend.domain.port.token.SessionStore;
import com.sajitar.backend.domain.port.token.TokenIssuer;
import com.sajitar.backend.domain.validation.Limit;
import com.sajitar.backend.domain.validation.profile.Birthday;

import jakarta.validation.ConstraintViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("SignInTokenUseCase")
class SignInTokenUseCaseTest {

    @Mock
    private ProfileRepository profiles;

    @Mock
    private CheckerRepository checkers;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private TokenIssuer tokens;

    @Mock
    private SessionStore sessions;

    @Mock
    private AttemptLimiter attempts;

    private SignInTokenUseCase useCase;

    @BeforeAll
    static void configureValidation() {
        Birthday.BirthdayValidator.configure(18);
        Limit.LimitValidator.configure(100);
    }

    @BeforeEach
    void setUp() {
        useCase = new SignInTokenUseCase(
                profiles,
                checkers,
                passwordHasher,
                tokens,
                sessions,
                attempts,
                TokenUseCaseFixture.CLOCK,
                TokenUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Sem refresh no corpo, abre sessão só com access")
    void opensSessionWithAccessOnly() {
        final var command = command(false);
        final var profile = TokenUseCaseFixture.persistedProfile();
        final var access = TokenUseCaseFixture.access();
        credentialsAccepted(profile);
        when(tokens.issueAccess(TokenUseCaseFixture.NOW)).thenReturn(access);

        final var issued = useCase.execute(command);

        assertThat(issued.access()).isEqualTo(access);
        assertThat(issued.hasRefresh()).isFalse();
        final var session = capturedSession();
        assertThat(session.id()).isEqualTo(issued.sessionId());
        assertThat(session.profileId()).isEqualTo(profile.id());
        assertThat(session.accessId()).isEqualTo(access.id());
        assertThat(session.refreshId()).isNull();
        verify(sessions).open(session, access.claims(), null, TokenUseCaseFixture.CLIENT);
        verify(tokens, never()).issueRefresh(any(), any());
    }

    @Test
    @DisplayName("Com refresh no corpo, emite o par na mesma sessão")
    void opensSessionWithPair() {
        final var command = command(true);
        final var profile = TokenUseCaseFixture.persistedProfile();
        final var access = TokenUseCaseFixture.access();
        final var refresh = TokenUseCaseFixture.refresh();
        credentialsAccepted(profile);
        when(tokens.issueAccess(TokenUseCaseFixture.NOW)).thenReturn(access);
        when(tokens.issueRefresh(eq(TokenUseCaseFixture.NOW), any())).thenReturn(refresh);

        final var issued = useCase.execute(command);

        assertThat(issued.hasRefresh()).isTrue();
        assertThat(issued.refresh()).isEqualTo(refresh);
        final var session = capturedSession();
        assertThat(session.refreshId()).isEqualTo(refresh.id());
        verify(sessions).open(session, access.claims(), refresh.claims(), TokenUseCaseFixture.CLIENT);
        verify(tokens).issueRefresh(TokenUseCaseFixture.NOW, session.bornAt());
    }

    @Test
    @DisplayName("Perfil com VERIFY_EMAIL não abre sessão")
    void refusesUnverifiedEmail() {
        final var command = command(true);
        final var profile = TokenUseCaseFixture.persistedProfile();
        when(profiles.findByEmail(command.email())).thenReturn(Optional.of(profile));
        when(passwordHasher.matches(command.password(), profile.password())).thenReturn(true);
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL))
                .thenReturn(Optional.of(TokenUseCaseFixture.verifyEmailChecker()));

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(EmailNotVerifiedException.class);
        assertThat(((EmailNotVerifiedException) thrown).content().get("email"))
                .containsExactly(EmailNotVerifiedException.MESSAGE_KEY);
        verify(sessions, never()).open(any(), any(), any(), any());
        verify(tokens, never()).issueAccess(any());
    }

    @Test
    @DisplayName("E-mail inexistente não revela nada além de credenciais inválidas")
    void refusesUnknownEmail() {
        final var command = command(false);
        when(profiles.findByEmail(command.email())).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(InvalidCredentialsException.class);
        assertThat(((InvalidCredentialsException) thrown).content().get("credentials"))
                .containsExactly(InvalidCredentialsException.MESSAGE_KEY);
        verify(passwordHasher, never()).matches(any(), any());
        verify(sessions, never()).open(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Senha incorreta não abre sessão")
    void refusesWrongPassword() {
        final var command = command(false);
        final var profile = TokenUseCaseFixture.persistedProfile();
        when(profiles.findByEmail(command.email())).thenReturn(Optional.of(profile));
        when(passwordHasher.matches(command.password(), profile.password())).thenReturn(false);

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(InvalidCredentialsException.class);
        verify(checkers, never()).findByProfileIdAndType(any(), any());
        verify(sessions, never()).open(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Limite por endereço barra antes do repositório")
    void refusesWhenAddressLimitIsExceeded() {
        when(attempts.register(AttemptScope.CREDENTIALS, TokenUseCaseFixture.ADDRESS))
                .thenReturn(Optional.of(Duration.ofSeconds(12)));
        when(attempts.register(AttemptScope.CREDENTIALS, TokenUseCaseFixture.EMAIL)).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(command(false)));

        assertThat(thrown).isInstanceOf(TooManyAttemptsException.class);
        assertThat(((TooManyAttemptsException) thrown).retryAfterSeconds()).isEqualTo(12L);
        verify(profiles, never()).findByEmail(any());
        verify(sessions, never()).open(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Limite por e-mail barra antes do repositório mesmo se o endereço ainda cabe")
    void refusesWhenEmailLimitIsExceeded() {
        when(attempts.register(AttemptScope.CREDENTIALS, TokenUseCaseFixture.ADDRESS)).thenReturn(Optional.empty());
        when(attempts.register(AttemptScope.CREDENTIALS, TokenUseCaseFixture.EMAIL))
                .thenReturn(Optional.of(Duration.ofMillis(1500)));

        final var thrown = catchThrowable(() -> useCase.execute(command(false)));

        assertThat(thrown).isInstanceOf(TooManyAttemptsException.class);
        assertThat(((TooManyAttemptsException) thrown).retryAfterSeconds()).isEqualTo(2L);
        verify(profiles, never()).findByEmail(any());
    }

    @Test
    @DisplayName("Quando endereço e e-mail estouram, a espera é a maior das duas")
    void usesLongerWaitWhenBothLimitsAreExceeded() {
        when(attempts.register(AttemptScope.CREDENTIALS, TokenUseCaseFixture.ADDRESS))
                .thenReturn(Optional.of(Duration.ofSeconds(3)));
        when(attempts.register(AttemptScope.CREDENTIALS, TokenUseCaseFixture.EMAIL))
                .thenReturn(Optional.of(Duration.ofSeconds(9)));

        final var thrown = catchThrowable(() -> useCase.execute(command(false)));

        assertThat(thrown).isInstanceOf(TooManyAttemptsException.class);
        assertThat(((TooManyAttemptsException) thrown).retryAfterSeconds()).isEqualTo(9L);
        verify(profiles, never()).findByEmail(any());
    }

    @ParameterizedTest
    @ValueSource(strings = { "not-an-email", "User@Example.com" })
    @DisplayName("E-mail inválido barra antes de consultar o repositório")
    void validatesEmailBeforePorts(final String email) {
        final var command = command(email, TokenUseCaseFixture.PASSWORD, true);

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findByEmail(any());
        verify(attempts, never()).register(any(), any());
        verify(tokens, never()).issueAccess(any());
        verify(sessions, never()).open(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Senha curta barra antes de consultar o repositório")
    void validatesPasswordBeforePorts() {
        final var command = command(TokenUseCaseFixture.EMAIL, "1234567", false);

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getPropertyPath()).hasToString("password");
        verify(profiles, never()).findByEmail(any());
        verify(attempts, never()).register(any(), any());
        verify(sessions, never()).open(any(), any(), any(), any());
    }

    private void credentialsAccepted(final com.sajitar.backend.domain.model.profile.Profile profile) {
        when(profiles.findByEmail(profile.email())).thenReturn(Optional.of(profile));
        when(passwordHasher.matches(TokenUseCaseFixture.PASSWORD, profile.password())).thenReturn(true);
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL)).thenReturn(Optional.empty());
    }

    private Session capturedSession() {
        final var captor = ArgumentCaptor.forClass(Session.class);
        verify(sessions).open(captor.capture(), any(), any(), any());
        return captor.getValue();
    }

    private static SignInTokenCommand command(final boolean refresh) {
        return command(TokenUseCaseFixture.EMAIL, TokenUseCaseFixture.PASSWORD, refresh);
    }

    private static SignInTokenCommand command(final String email, final String password, final boolean refresh) {
        return new SignInTokenCommand(email, password, refresh, TokenUseCaseFixture.ADDRESS, TokenUseCaseFixture.CLIENT);
    }

}
