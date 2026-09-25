package com.sajitar.backend.application.usecase.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

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

import com.sajitar.backend.application.command.profile.RequestPasswordRecoveryCommand;
import com.sajitar.backend.configuration.LocaleConfiguration;
import com.sajitar.backend.configuration.ProfilePurgeProperties;
import com.sajitar.backend.domain.exception.MailUnavailableException;
import com.sajitar.backend.domain.exception.TooManyAttemptsException;
import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.model.mail.MailMessage;
import com.sajitar.backend.domain.model.token.AttemptScope;
import com.sajitar.backend.domain.port.Mailer;
import com.sajitar.backend.domain.port.checker.CheckerRepository;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.port.token.AttemptLimiter;

import jakarta.validation.ConstraintViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("RequestPasswordRecoveryUseCase")
class RequestPasswordRecoveryUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private ProfileRepository profiles;

    @Mock
    private CheckerRepository checkers;

    @Mock
    private Mailer mailer;

    @Mock
    private AttemptLimiter attempts;

    private RequestPasswordRecoveryUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RequestPasswordRecoveryUseCase(
                profiles,
                checkers,
                mailer,
                attempts,
                CLOCK,
                new ProfilePurgeProperties(48, 12, "UTC"),
                new LocaleConfiguration().messageSource(),
                ProfileUseCaseFixture.VALIDATOR);
    }

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    @DisplayName("Cria CHANGE_PASSWORD e envia o e-mail")
    void createsCheckerAndSendsMail() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        limitsAccepted(profile.email());
        when(profiles.findByEmail(profile.email())).thenReturn(Optional.of(profile));
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL)).thenReturn(Optional.empty());
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.CHANGE_PASSWORD)).thenReturn(Optional.empty());
        when(checkers.save(any(Checker.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(command());

        final var saved = ArgumentCaptor.forClass(Checker.class);
        verify(checkers).save(saved.capture());
        assertThat(saved.getValue().profileId()).isEqualTo(profile.id());
        assertThat(saved.getValue().type()).isEqualTo(Checker.Type.CHANGE_PASSWORD);
        assertThat(saved.getValue().code()).matches("^[0-9]{6}$");
        final var mail = ArgumentCaptor.forClass(MailMessage.class);
        verify(mailer).send(mail.capture());
        assertThat(mail.getValue().to()).isEqualTo(profile.email());
        assertThat(mail.getValue().subject()).isEqualTo("Your Sajitar code · 2026-01-01 10:00:00 UTC");
        assertThat(mail.getValue().subject()).doesNotContain(saved.getValue().code());
        assertThat(mail.getValue().body()).contains(saved.getValue().code());
        assertThat(mail.getValue().body()).contains("You have 12 hours from the first request");
    }

    @Test
    @DisplayName("Gira o código vigente e envia o e-mail")
    void rotatesCodeAndSendsMail() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        final var checker = currentChangePassword(profile.id());
        limitsAccepted(profile.email());
        when(profiles.findByEmail(profile.email())).thenReturn(Optional.of(profile));
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL)).thenReturn(Optional.empty());
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.CHANGE_PASSWORD))
                .thenReturn(Optional.of(checker));
        when(checkers.save(any(Checker.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(command());

        final var saved = ArgumentCaptor.forClass(Checker.class);
        verify(checkers).save(saved.capture());
        assertThat(saved.getValue().id()).isEqualTo(checker.id());
        assertThat(saved.getValue().code()).isNotEqualTo(checker.code());
        assertThat(saved.getValue().code()).matches("^[0-9]{6}$");
        final var mail = ArgumentCaptor.forClass(MailMessage.class);
        verify(mailer).send(mail.capture());
        assertThat(mail.getValue().body()).contains(saved.getValue().code());
        assertThat(mail.getValue().body()).doesNotContain(checker.code());
    }

    @Test
    @DisplayName("E-mail inexistente responde em silêncio")
    void unknownEmailIsSilent() {
        limitsAccepted(ProfileUseCaseFixture.EMAIL);
        when(profiles.findByEmail(ProfileUseCaseFixture.EMAIL)).thenReturn(Optional.empty());

        assertThatCode(() -> useCase.execute(command())).doesNotThrowAnyException();

        verify(checkers, never()).findByProfileIdAndType(any(), any());
        verify(checkers, never()).save(any());
        verify(mailer, never()).send(any());
    }

    @Test
    @DisplayName("Perfil com VERIFY_EMAIL não envia e-mail")
    void unverifiedProfileIsSilent() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        limitsAccepted(profile.email());
        when(profiles.findByEmail(profile.email())).thenReturn(Optional.of(profile));
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL))
                .thenReturn(Optional.of(Checker.create(profile.id(), Checker.Type.VERIFY_EMAIL)));

        assertThatCode(() -> useCase.execute(command())).doesNotThrowAnyException();

        verify(checkers, never()).findByProfileIdAndType(profile.id(), Checker.Type.CHANGE_PASSWORD);
        verify(checkers, never()).save(any());
        verify(mailer, never()).send(any());
    }

    @Test
    @DisplayName("CHANGE_PASSWORD vencido não gira nem envia")
    void expiredCheckerIsSilent() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        final var expired = new Checker(
                Checker.uuidV7At(NOW.minus(Duration.ofHours(13))),
                profile.id(),
                Checker.Type.CHANGE_PASSWORD,
                "123456",
                null);
        limitsAccepted(profile.email());
        when(profiles.findByEmail(profile.email())).thenReturn(Optional.of(profile));
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL)).thenReturn(Optional.empty());
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.CHANGE_PASSWORD))
                .thenReturn(Optional.of(expired));

        assertThatCode(() -> useCase.execute(command())).doesNotThrowAnyException();

        verify(checkers, never()).save(any());
        verify(mailer, never()).send(any());
    }

    @Test
    @DisplayName("Falha de envio propaga MailUnavailableException")
    void mailFailurePropagates() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        limitsAccepted(profile.email());
        when(profiles.findByEmail(profile.email())).thenReturn(Optional.of(profile));
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL)).thenReturn(Optional.empty());
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.CHANGE_PASSWORD)).thenReturn(Optional.empty());
        when(checkers.save(any(Checker.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new MailUnavailableException()).when(mailer).send(any(MailMessage.class));

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(MailUnavailableException.class);
        verify(checkers).save(any(Checker.class));
    }

    @Test
    @DisplayName("Limite por endereço barra antes do repositório")
    void refusesWhenAddressLimitIsExceeded() {
        when(attempts.register(AttemptScope.CREDENTIALS, ProfileUseCaseFixture.ADDRESS))
                .thenReturn(Optional.of(Duration.ofSeconds(12)));
        when(attempts.register(AttemptScope.CREDENTIALS, ProfileUseCaseFixture.EMAIL)).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(TooManyAttemptsException.class);
        assertThat(((TooManyAttemptsException) thrown).retryAfterSeconds()).isEqualTo(12L);
        verify(profiles, never()).findByEmail(any());
        verify(mailer, never()).send(any());
    }

    @Test
    @DisplayName("Limite por e-mail barra mesmo se o endereço ainda cabe")
    void refusesWhenEmailLimitIsExceeded() {
        when(attempts.register(AttemptScope.CREDENTIALS, ProfileUseCaseFixture.ADDRESS)).thenReturn(Optional.empty());
        when(attempts.register(AttemptScope.CREDENTIALS, ProfileUseCaseFixture.EMAIL))
                .thenReturn(Optional.of(Duration.ofMillis(1500)));

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(TooManyAttemptsException.class);
        assertThat(((TooManyAttemptsException) thrown).retryAfterSeconds()).isEqualTo(2L);
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

    @ParameterizedTest
    @ValueSource(strings = { "not-an-email", "User@Example.com" })
    @DisplayName("E-mail inválido barra antes de consultar o repositório")
    void validatesEmailBeforePorts(final String email) {
        final var thrown = catchThrowable(
                () -> useCase.execute(new RequestPasswordRecoveryCommand(email, ProfileUseCaseFixture.ADDRESS)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findByEmail(any());
        verify(attempts, never()).register(any(), any());
        verify(mailer, never()).send(any());
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
                "123456",
                null);
    }

    private static RequestPasswordRecoveryCommand command() {
        return new RequestPasswordRecoveryCommand(ProfileUseCaseFixture.EMAIL, ProfileUseCaseFixture.ADDRESS);
    }

}
