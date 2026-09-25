package com.sajitar.backend.application.usecase.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;

import com.sajitar.backend.application.command.token.ResendVerifyEmailCommand;
import com.sajitar.backend.configuration.LocaleConfiguration;
import com.sajitar.backend.configuration.ProfilePurgeProperties;
import com.sajitar.backend.domain.exception.InvalidCredentialsException;
import com.sajitar.backend.domain.exception.MailUnavailableException;
import com.sajitar.backend.domain.exception.TooManyAttemptsException;
import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.model.mail.MailMessage;
import com.sajitar.backend.domain.model.token.AttemptScope;
import com.sajitar.backend.domain.port.Mailer;
import com.sajitar.backend.domain.port.PasswordHasher;
import com.sajitar.backend.domain.port.checker.CheckerRepository;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.port.token.AttemptLimiter;

import jakarta.validation.ConstraintViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResendVerifyEmailUseCase")
class ResendVerifyEmailUseCaseTest {

    @Mock
    private ProfileRepository profiles;

    @Mock
    private CheckerRepository checkers;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private Mailer mailer;

    @Mock
    private AttemptLimiter attempts;

    private ResendVerifyEmailUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ResendVerifyEmailUseCase(
                profiles,
                checkers,
                passwordHasher,
                mailer,
                attempts,
                TokenUseCaseFixture.CLOCK,
                new ProfilePurgeProperties(48, 12, "UTC"),
                new LocaleConfiguration().messageSource(),
                TokenUseCaseFixture.VALIDATOR);
    }

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    @DisplayName("Gira o código e envia o e-mail")
    void rotatesCodeAndSendsMail() {
        final var profile = TokenUseCaseFixture.persistedProfile();
        final var checker = TokenUseCaseFixture.verifyEmailChecker();
        credentialsAccepted(profile, Optional.of(checker));
        when(checkers.save(any(Checker.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(command());

        final var saved = ArgumentCaptor.forClass(Checker.class);
        verify(checkers).save(saved.capture());
        assertThat(saved.getValue().id()).isEqualTo(checker.id());
        assertThat(saved.getValue().code()).isNotEqualTo(checker.code());
        assertThat(saved.getValue().code()).matches("^[0-9]{6}$");
        final var mail = ArgumentCaptor.forClass(MailMessage.class);
        verify(mailer).send(mail.capture());
        assertThat(mail.getValue().to()).isEqualTo(profile.email());
        assertThat(mail.getValue().subject()).isEqualTo("Your Sajitar code · 2026-01-01 10:00:00 UTC");
        assertThat(mail.getValue().subject()).doesNotContain(saved.getValue().code());
        assertThat(mail.getValue().body()).contains(saved.getValue().code());
        assertThat(mail.getValue().body()).doesNotContain(checker.code());
        assertThat(mail.getValue().body()).contains("You have 48 hours from account creation");
    }

    @Test
    @DisplayName("Perfil já verificado não envia e-mail")
    void alreadyVerifiedDoesNotSendMail() {
        credentialsAccepted(TokenUseCaseFixture.persistedProfile(), Optional.empty());

        assertThatCode(() -> useCase.execute(command())).doesNotThrowAnyException();

        verify(checkers, never()).save(any());
        verify(mailer, never()).send(any());
    }

    @Test
    @DisplayName("E-mail inexistente não revela nada além de credenciais inválidas")
    void refusesUnknownEmail() {
        when(profiles.findByEmail(TokenUseCaseFixture.EMAIL)).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(InvalidCredentialsException.class);
        verify(passwordHasher, never()).matches(any(), any());
        verify(checkers, never()).findByProfileIdAndType(any(), any());
        verify(mailer, never()).send(any());
    }

    @Test
    @DisplayName("Senha incorreta não envia")
    void refusesWrongPassword() {
        final var profile = TokenUseCaseFixture.persistedProfile();
        when(profiles.findByEmail(profile.email())).thenReturn(Optional.of(profile));
        when(passwordHasher.matches(TokenUseCaseFixture.PASSWORD, profile.password())).thenReturn(false);

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(InvalidCredentialsException.class);
        verify(checkers, never()).findByProfileIdAndType(any(), any());
        verify(checkers, never()).save(any());
        verify(mailer, never()).send(any());
    }

    @Test
    @DisplayName("Falha de envio propaga MailUnavailableException")
    void mailFailurePropagates() {
        final var profile = TokenUseCaseFixture.persistedProfile();
        credentialsAccepted(profile, Optional.of(TokenUseCaseFixture.verifyEmailChecker()));
        when(checkers.save(any(Checker.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new MailUnavailableException()).when(mailer).send(any(MailMessage.class));

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(MailUnavailableException.class);
        verify(checkers).save(any(Checker.class));
    }

    @Test
    @DisplayName("Limite por endereço barra antes do repositório")
    void refusesWhenAddressLimitIsExceeded() {
        when(attempts.register(AttemptScope.CREDENTIALS, TokenUseCaseFixture.ADDRESS))
                .thenReturn(Optional.of(Duration.ofSeconds(12)));
        when(attempts.register(AttemptScope.CREDENTIALS, TokenUseCaseFixture.EMAIL)).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(TooManyAttemptsException.class);
        assertThat(((TooManyAttemptsException) thrown).retryAfterSeconds()).isEqualTo(12L);
        verify(profiles, never()).findByEmail(any());
        verify(mailer, never()).send(any());
    }

    @ParameterizedTest
    @ValueSource(strings = { "not-an-email", "User@Example.com" })
    @DisplayName("E-mail inválido barra antes de consultar o repositório")
    void validatesEmailBeforePorts(final String email) {
        final var thrown = catchThrowable(
                () -> useCase.execute(new ResendVerifyEmailCommand(email, TokenUseCaseFixture.PASSWORD, TokenUseCaseFixture.ADDRESS)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findByEmail(any());
        verify(attempts, never()).register(any(), any());
        verify(mailer, never()).send(any());
    }

    @Test
    @DisplayName("Senha curta barra antes de consultar o repositório")
    void validatesPasswordBeforePorts() {
        final var thrown = catchThrowable(() -> useCase.execute(
                new ResendVerifyEmailCommand(TokenUseCaseFixture.EMAIL, "1234567", TokenUseCaseFixture.ADDRESS)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findByEmail(any());
        verify(attempts, never()).register(any(), any());
        verify(mailer, never()).send(any());
    }

    private void credentialsAccepted(
            final com.sajitar.backend.domain.model.profile.Profile profile,
            final Optional<Checker> checker) {
        when(profiles.findByEmail(profile.email())).thenReturn(Optional.of(profile));
        when(passwordHasher.matches(TokenUseCaseFixture.PASSWORD, profile.password())).thenReturn(true);
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL)).thenReturn(checker);
    }

    private static ResendVerifyEmailCommand command() {
        return new ResendVerifyEmailCommand(
                TokenUseCaseFixture.EMAIL,
                TokenUseCaseFixture.PASSWORD,
                TokenUseCaseFixture.ADDRESS);
    }

}
