package com.sajitar.backend.application.usecase.profile;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;

import com.sajitar.backend.application.command.profile.RequestChangeEmailCommand;
import com.sajitar.backend.configuration.LocaleConfiguration;
import com.sajitar.backend.configuration.ProfilePurgeProperties;
import com.sajitar.backend.domain.exception.EmailNotVerifiedException;
import com.sajitar.backend.domain.exception.InvalidCheckerVerificationException;
import com.sajitar.backend.domain.exception.MailUnavailableException;
import com.sajitar.backend.domain.exception.ProfileNotFoundException;
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
@DisplayName("RequestChangeEmailUseCase")
class RequestChangeEmailUseCaseTest {

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

    private RequestChangeEmailUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RequestChangeEmailUseCase(
                profiles,
                checkers,
                mailer,
                attempts,
                CLOCK,
                new ProfilePurgeProperties(48, 12, 12, "UTC"),
                new LocaleConfiguration().messageSource(),
                ProfileUseCaseFixture.VALIDATOR);
    }

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    @DisplayName("Cria CHANGE_EMAIL e envia o código ao e-mail vigente")
    void createsCheckerAndSendsMailToCurrentEmail() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        limitsAccepted(profile.email());
        when(profiles.findById(profile.id())).thenReturn(Optional.of(profile));
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL)).thenReturn(Optional.empty());
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.CHANGE_EMAIL)).thenReturn(Optional.empty());
        when(checkers.save(any(Checker.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(command());

        final var saved = ArgumentCaptor.forClass(Checker.class);
        verify(checkers).save(saved.capture());
        assertThat(saved.getValue().profileId()).isEqualTo(profile.id());
        assertThat(saved.getValue().type()).isEqualTo(Checker.Type.CHANGE_EMAIL);
        assertThat(saved.getValue().payload()).isNull();
        assertThat(saved.getValue().code()).matches("^[0-9]{6}$");
        final var mail = ArgumentCaptor.forClass(MailMessage.class);
        verify(mailer).send(mail.capture());
        assertThat(mail.getValue().to()).isEqualTo(profile.email());
        assertThat(mail.getValue().subject()).doesNotContain(saved.getValue().code());
        assertThat(mail.getValue().body()).contains(saved.getValue().code());
        assertThat(mail.getValue().body()).contains("Confirm this email change");
    }

    @Test
    @DisplayName("Gira o código com payload nulo e reenvia ao e-mail vigente")
    void rotatesNullPayloadAndSendsToCurrentEmail() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        final var checker = currentChangeEmail(profile.id(), null);
        limitsAccepted(profile.email());
        when(profiles.findById(profile.id())).thenReturn(Optional.of(profile));
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL)).thenReturn(Optional.empty());
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.CHANGE_EMAIL)).thenReturn(Optional.of(checker));
        when(checkers.save(any(Checker.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(command());

        final var saved = ArgumentCaptor.forClass(Checker.class);
        verify(checkers).save(saved.capture());
        assertThat(saved.getValue().id()).isEqualTo(checker.id());
        assertThat(saved.getValue().payload()).isNull();
        assertThat(saved.getValue().code()).isNotEqualTo(checker.code());
        final var mail = ArgumentCaptor.forClass(MailMessage.class);
        verify(mailer).send(mail.capture());
        assertThat(mail.getValue().to()).isEqualTo(profile.email());
        assertThat(mail.getValue().body()).contains("Confirm this email change");
    }

    @Test
    @DisplayName("Com payload gravado limpa o payload, gira o código e reenvia ao e-mail vigente")
    void filledPayloadRestartsAndSendsToCurrentEmail() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        final var checker = currentChangeEmail(profile.id(), ProfileUseCaseFixture.NEW_EMAIL);
        limitsAccepted(profile.email());
        when(profiles.findById(profile.id())).thenReturn(Optional.of(profile));
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL)).thenReturn(Optional.empty());
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.CHANGE_EMAIL)).thenReturn(Optional.of(checker));
        when(checkers.save(any(Checker.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(command());

        final var saved = ArgumentCaptor.forClass(Checker.class);
        verify(checkers).save(saved.capture());
        assertThat(saved.getValue().id()).isEqualTo(checker.id());
        assertThat(saved.getValue().payload()).isNull();
        assertThat(saved.getValue().code()).isNotEqualTo(checker.code());
        final var mail = ArgumentCaptor.forClass(MailMessage.class);
        verify(mailer).send(mail.capture());
        assertThat(mail.getValue().to()).isEqualTo(profile.email());
        assertThat(mail.getValue().body()).contains("Confirm this email change");
    }

    @Test
    @DisplayName("Perfil ausente responde 404 sem limiter")
    void missingProfileReturns404() {
        when(profiles.findById(ProfileUseCaseFixture.ID)).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(ProfileNotFoundException.class);
        verify(attempts, never()).register(any(), any());
        verify(mailer, never()).send(any());
    }

    @Test
    @DisplayName("VERIFY_EMAIL responde 403 sem girar")
    void unverifiedEmailDoesNotRotate() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        limitsAccepted(profile.email());
        when(profiles.findById(profile.id())).thenReturn(Optional.of(profile));
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL))
                .thenReturn(Optional.of(Checker.create(profile.id(), Checker.Type.VERIFY_EMAIL)));

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(EmailNotVerifiedException.class);
        verify(checkers, never()).save(any());
        verify(mailer, never()).send(any());
    }

    @Test
    @DisplayName("Checker vencido responde 401 sem girar")
    void expiredCheckerReturnsInvalidCode() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        final var expired = new Checker(
                Checker.uuidV7At(NOW.minus(Duration.ofHours(13))),
                profile.id(),
                Checker.Type.CHANGE_EMAIL,
                "123456",
                null);
        limitsAccepted(profile.email());
        when(profiles.findById(profile.id())).thenReturn(Optional.of(profile));
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL)).thenReturn(Optional.empty());
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.CHANGE_EMAIL)).thenReturn(Optional.of(expired));

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(InvalidCheckerVerificationException.class);
        verify(checkers, never()).save(any());
        verify(mailer, never()).send(any());
    }

    @Test
    @DisplayName("Id nulo barra antes do repositório")
    void validatesProfileIdBeforePorts() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new RequestChangeEmailCommand(null, ProfileUseCaseFixture.ADDRESS)));

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
        assertThat(((TooManyAttemptsException) thrown).retryAfterSeconds()).isEqualTo(12L);
        verify(mailer, never()).send(any());
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
        assertThat(((TooManyAttemptsException) thrown).retryAfterSeconds()).isEqualTo(9L);
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
    }

    @Test
    @DisplayName("Correio fora do ar propaga MailUnavailableException")
    void mailFailurePropagates() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        limitsAccepted(profile.email());
        when(profiles.findById(profile.id())).thenReturn(Optional.of(profile));
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL)).thenReturn(Optional.empty());
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.CHANGE_EMAIL)).thenReturn(Optional.empty());
        when(checkers.save(any(Checker.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new MailUnavailableException()).when(mailer).send(any());

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(MailUnavailableException.class);
    }

    private void limitsAccepted(final String email) {
        when(attempts.register(AttemptScope.CREDENTIALS, ProfileUseCaseFixture.ADDRESS)).thenReturn(Optional.empty());
        when(attempts.register(AttemptScope.CREDENTIALS, email)).thenReturn(Optional.empty());
    }

    private static Checker currentChangeEmail(final UUID profileId, final String payload) {
        return new Checker(
                Checker.uuidV7At(NOW.minus(Duration.ofHours(1))),
                profileId,
                Checker.Type.CHANGE_EMAIL,
                "123456",
                payload);
    }

    private static RequestChangeEmailCommand command() {
        return new RequestChangeEmailCommand(ProfileUseCaseFixture.ID, ProfileUseCaseFixture.ADDRESS);
    }

}
