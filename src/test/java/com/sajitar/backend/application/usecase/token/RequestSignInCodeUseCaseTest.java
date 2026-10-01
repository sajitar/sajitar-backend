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

import com.sajitar.backend.application.command.token.RequestSignInCodeCommand;
import com.sajitar.backend.configuration.LocaleConfiguration;
import com.sajitar.backend.configuration.ProfilePurgeProperties;
import com.sajitar.backend.domain.exception.InvalidCredentialsException;
import com.sajitar.backend.domain.exception.MailUnavailableException;
import com.sajitar.backend.domain.exception.TooManyAttemptsException;
import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.model.mail.MailMessage;
import com.sajitar.backend.domain.model.profile.Profile;
import com.sajitar.backend.domain.model.token.AttemptScope;
import com.sajitar.backend.domain.port.Mailer;
import com.sajitar.backend.domain.port.PasswordHasher;
import com.sajitar.backend.domain.port.checker.CheckerRepository;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.port.token.AttemptLimiter;

import jakarta.validation.ConstraintViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("RequestSignInCodeUseCase")
class RequestSignInCodeUseCaseTest {

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

    private RequestSignInCodeUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RequestSignInCodeUseCase(
                profiles,
                checkers,
                passwordHasher,
                mailer,
                attempts,
                TokenUseCaseFixture.CLOCK,
                new ProfilePurgeProperties(30, 30, 30, 30, 30, "UTC"),
                new LocaleConfiguration().messageSource(),
                TokenUseCaseFixture.VALIDATOR);
    }

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    @DisplayName("Cria o checker e envia o e-mail quando o segundo fator é obrigatório")
    void createsCheckerAndSendsMail() {
        final var profile = TokenUseCaseFixture.persistedProfile().withTwoFactor(true);
        credentialsAccepted(profile);
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL)).thenReturn(Optional.empty());
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.SIGN_IN)).thenReturn(Optional.empty());
        when(checkers.save(any(Checker.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(command());

        final var saved = ArgumentCaptor.forClass(Checker.class);
        verify(checkers).save(saved.capture());
        assertThat(saved.getValue().type()).isEqualTo(Checker.Type.SIGN_IN);
        assertThat(saved.getValue().profileId()).isEqualTo(profile.id());
        assertThat(saved.getValue().code()).matches("^[0-9]{6}$");
        final var mail = ArgumentCaptor.forClass(MailMessage.class);
        verify(mailer).send(mail.capture());
        assertThat(mail.getValue().to()).isEqualTo(profile.email());
        assertThat(mail.getValue().subject()).isEqualTo("Your Sajitar code · 2026-01-01 10:00:00 UTC");
        assertThat(mail.getValue().subject()).doesNotContain(saved.getValue().code());
        assertThat(mail.getValue().body()).contains(saved.getValue().code());
        assertThat(mail.getValue().body()).contains(
                "You have 30 minutes from the first request; after that the code expires and you must start again.");
    }

    @Test
    @DisplayName("Gira o código vigente e envia o e-mail")
    void rotatesExistingCheckerAndSendsMail() {
        final var profile = TokenUseCaseFixture.persistedProfile().withType(Profile.Type.MASTER).withTwoFactor(true);
        final var checker = TokenUseCaseFixture.signInChecker();
        credentialsAccepted(profile);
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL)).thenReturn(Optional.empty());
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.SIGN_IN)).thenReturn(Optional.of(checker));
        when(checkers.save(any(Checker.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(command());

        final var saved = ArgumentCaptor.forClass(Checker.class);
        verify(checkers).save(saved.capture());
        assertThat(saved.getValue().id()).isEqualTo(checker.id());
        assertThat(saved.getValue().code()).isNotEqualTo(checker.code());
        verify(mailer).send(any(MailMessage.class));
    }

    @Test
    @DisplayName("SIGN_IN vencido não gira nem envia")
    void expiredCheckerIsSilent() {
        final var profile = TokenUseCaseFixture.persistedProfile().withTwoFactor(true);
        final var expired = new Checker(
                Checker.uuidV7At(TokenUseCaseFixture.NOW.minus(Duration.ofMinutes(31))),
                profile.id(),
                Checker.Type.SIGN_IN,
                "123456",
                null);
        credentialsAccepted(profile);
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL)).thenReturn(Optional.empty());
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.SIGN_IN)).thenReturn(Optional.of(expired));

        assertThatCode(() -> useCase.execute(command())).doesNotThrowAnyException();

        verify(checkers, never()).save(any());
        verify(mailer, never()).send(any());
    }

    @Test
    @DisplayName("Segundo fator não obrigatório não envia e-mail")
    void notRequiredDoesNotSendMail() {
        credentialsAccepted(TokenUseCaseFixture.persistedProfile());
        when(checkers.findByProfileIdAndType(TokenUseCaseFixture.PROFILE_ID, Checker.Type.VERIFY_EMAIL))
                .thenReturn(Optional.empty());

        assertThatCode(() -> useCase.execute(command())).doesNotThrowAnyException();

        verify(checkers, never()).save(any());
        verify(mailer, never()).send(any());
    }

    @Test
    @DisplayName("VERIFY_EMAIL presente não envia e-mail")
    void verifyEmailPresentDoesNotSendMail() {
        final var profile = TokenUseCaseFixture.persistedProfile().withTwoFactor(true);
        credentialsAccepted(profile);
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL))
                .thenReturn(Optional.of(TokenUseCaseFixture.verifyEmailChecker()));

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
        verify(mailer, never()).send(any());
    }

    @Test
    @DisplayName("Falha de envio propaga MailUnavailableException")
    void mailFailurePropagates() {
        final var profile = TokenUseCaseFixture.persistedProfile().withTwoFactor(true);
        credentialsAccepted(profile);
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL)).thenReturn(Optional.empty());
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.SIGN_IN)).thenReturn(Optional.empty());
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
                () -> useCase.execute(new RequestSignInCodeCommand(email, TokenUseCaseFixture.PASSWORD, TokenUseCaseFixture.ADDRESS)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findByEmail(any());
        verify(attempts, never()).register(any(), any());
        verify(mailer, never()).send(any());
    }

    @Test
    @DisplayName("Senha curta barra antes de consultar o repositório")
    void validatesPasswordBeforePorts() {
        final var thrown = catchThrowable(() -> useCase.execute(
                new RequestSignInCodeCommand(TokenUseCaseFixture.EMAIL, "1234567", TokenUseCaseFixture.ADDRESS)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findByEmail(any());
        verify(attempts, never()).register(any(), any());
        verify(mailer, never()).send(any());
    }

    private void credentialsAccepted(final Profile profile) {
        when(profiles.findByEmail(profile.email())).thenReturn(Optional.of(profile));
        when(passwordHasher.matches(TokenUseCaseFixture.PASSWORD, profile.password())).thenReturn(true);
    }

    private static RequestSignInCodeCommand command() {
        return new RequestSignInCodeCommand(
                TokenUseCaseFixture.EMAIL,
                TokenUseCaseFixture.PASSWORD,
                TokenUseCaseFixture.ADDRESS);
    }

}
