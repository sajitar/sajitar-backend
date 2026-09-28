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

import com.sajitar.backend.application.command.profile.ConfirmChangeEmailCommand;
import com.sajitar.backend.configuration.LocaleConfiguration;
import com.sajitar.backend.configuration.ProfilePurgeProperties;
import com.sajitar.backend.domain.exception.EmailAlreadyRegisteredException;
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
@DisplayName("ConfirmChangeEmailUseCase")
class ConfirmChangeEmailUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private static final String CODE = "123456";

    @Mock
    private ProfileRepository profiles;

    @Mock
    private CheckerRepository checkers;

    @Mock
    private Mailer mailer;

    @Mock
    private AttemptLimiter attempts;

    private ConfirmChangeEmailUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ConfirmChangeEmailUseCase(
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
    @DisplayName("Grava o payload, gira o código e envia ao e-mail novo")
    void storesPayloadRotatesAndMailsNewAddress() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        final var checker = currentChangeEmail(profile.id(), null);
        ready(profile);
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.CHANGE_EMAIL))
                .thenReturn(Optional.of(checker));
        when(profiles.findByEmail(ProfileUseCaseFixture.NEW_EMAIL)).thenReturn(Optional.empty());
        when(checkers.save(any(Checker.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(command());

        final var saved = ArgumentCaptor.forClass(Checker.class);
        verify(checkers).save(saved.capture());
        assertThat(saved.getValue().id()).isEqualTo(checker.id());
        assertThat(saved.getValue().payload()).isEqualTo(ProfileUseCaseFixture.NEW_EMAIL);
        assertThat(saved.getValue().code()).isNotEqualTo(checker.code());
        final var mail = ArgumentCaptor.forClass(MailMessage.class);
        verify(mailer).send(mail.capture());
        assertThat(mail.getValue().to()).isEqualTo(ProfileUseCaseFixture.NEW_EMAIL);
        assertThat(mail.getValue().body()).contains(saved.getValue().code());
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("Perfil ausente responde 404")
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
        ready(profile);
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL))
                .thenReturn(Optional.of(Checker.create(profile.id(), Checker.Type.VERIFY_EMAIL)));

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(EmailNotVerifiedException.class);
        verify(checkers, never()).save(any());
        verify(mailer, never()).send(any());
    }

    @Test
    @DisplayName("E-mail novo igual ao vigente responde 400 sem consultar o checker")
    void sameEmailReturns400() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        when(profiles.findById(profile.id())).thenReturn(Optional.of(profile));
        when(attempts.register(AttemptScope.CREDENTIALS, ProfileUseCaseFixture.ADDRESS)).thenReturn(Optional.empty());
        when(attempts.register(AttemptScope.CREDENTIALS, profile.email())).thenReturn(Optional.empty());
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL)).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(new ConfirmChangeEmailCommand(
                profile.id(),
                CODE,
                profile.email(),
                ProfileUseCaseFixture.ADDRESS)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(checkers, never()).findByProfileIdAndType(profile.id(), Checker.Type.CHANGE_EMAIL);
        verify(checkers, never()).save(any());
    }

    @Test
    @DisplayName("Checker ausente responde 401 em code")
    void missingCheckerReturnsInvalidCode() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        ready(profile);
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.CHANGE_EMAIL)).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(InvalidCheckerVerificationException.class);
        verify(checkers, never()).save(any());
        verify(mailer, never()).send(any());
    }

    @Test
    @DisplayName("Checker vencido responde 401 em code")
    void expiredCheckerReturnsInvalidCode() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        final var expired = new Checker(
                Checker.uuidV7At(NOW.minus(Duration.ofHours(13))),
                profile.id(),
                Checker.Type.CHANGE_EMAIL,
                CODE,
                null);
        ready(profile);
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.CHANGE_EMAIL)).thenReturn(Optional.of(expired));

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(InvalidCheckerVerificationException.class);
        verify(checkers, never()).save(any());
    }

    @Test
    @DisplayName("Payload já preenchido responde 401 sem girar")
    void filledPayloadReturnsInvalidCode() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        final var checker = currentChangeEmail(profile.id(), ProfileUseCaseFixture.NEW_EMAIL);
        ready(profile);
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.CHANGE_EMAIL)).thenReturn(Optional.of(checker));

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(InvalidCheckerVerificationException.class);
        verify(checkers, never()).save(any());
        verify(mailer, never()).send(any());
    }

    @Test
    @DisplayName("Código divergente responde 401 e não altera o checker")
    void mismatchedCodeDoesNotChangeChecker() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        final var checker = currentChangeEmail(profile.id(), null);
        ready(profile);
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.CHANGE_EMAIL)).thenReturn(Optional.of(checker));

        final var thrown = catchThrowable(() -> useCase.execute(new ConfirmChangeEmailCommand(
                profile.id(),
                "654321",
                ProfileUseCaseFixture.NEW_EMAIL,
                ProfileUseCaseFixture.ADDRESS)));

        assertThat(thrown).isInstanceOf(InvalidCheckerVerificationException.class);
        verify(checkers, never()).save(any());
        verify(mailer, never()).send(any());
    }

    @Test
    @DisplayName("E-mail de outro perfil responde 409 sem consumir o código")
    void takenEmailReturns409WithoutRotating() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        final var checker = currentChangeEmail(profile.id(), null);
        final var other = profile.withId(UUID.randomUUID()).withEmail(ProfileUseCaseFixture.NEW_EMAIL);
        ready(profile);
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.CHANGE_EMAIL)).thenReturn(Optional.of(checker));
        when(profiles.findByEmail(ProfileUseCaseFixture.NEW_EMAIL)).thenReturn(Optional.of(other));

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(EmailAlreadyRegisteredException.class);
        verify(checkers, never()).save(any());
        verify(mailer, never()).send(any());
    }

    @Test
    @DisplayName("Código mal formado barra antes do repositório")
    void validatesCodeBeforePorts() {
        final var thrown = catchThrowable(() -> useCase.execute(new ConfirmChangeEmailCommand(
                ProfileUseCaseFixture.ID,
                "12a456",
                ProfileUseCaseFixture.NEW_EMAIL,
                ProfileUseCaseFixture.ADDRESS)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findById(any());
        verify(attempts, never()).register(any(), any());
    }

    @Test
    @DisplayName("E-mail novo inválido barra antes do repositório")
    void validatesNewEmailBeforePorts() {
        final var thrown = catchThrowable(() -> useCase.execute(new ConfirmChangeEmailCommand(
                ProfileUseCaseFixture.ID,
                CODE,
                "not-an-email",
                ProfileUseCaseFixture.ADDRESS)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findById(any());
    }

    @Test
    @DisplayName("Limite por endereço barra depois de achar o perfil")
    void refusesWhenAddressLimitIsExceeded() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        when(profiles.findById(profile.id())).thenReturn(Optional.of(profile));
        when(attempts.register(AttemptScope.CREDENTIALS, ProfileUseCaseFixture.ADDRESS))
                .thenReturn(Optional.of(Duration.ofSeconds(12)));
        when(attempts.register(AttemptScope.CREDENTIALS, profile.email())).thenReturn(Optional.empty());
        when(attempts.register(AttemptScope.CREDENTIALS, ProfileUseCaseFixture.NEW_EMAIL)).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(TooManyAttemptsException.class);
        assertThat(((TooManyAttemptsException) thrown).retryAfterSeconds()).isEqualTo(12L);
        verify(mailer, never()).send(any());
    }

    @Test
    @DisplayName("Limite pelo e-mail novo barra mesmo se os demais cabem")
    void refusesWhenNewEmailLimitIsExceeded() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        when(profiles.findById(profile.id())).thenReturn(Optional.of(profile));
        when(attempts.register(AttemptScope.CREDENTIALS, ProfileUseCaseFixture.ADDRESS)).thenReturn(Optional.empty());
        when(attempts.register(AttemptScope.CREDENTIALS, profile.email())).thenReturn(Optional.empty());
        when(attempts.register(AttemptScope.CREDENTIALS, ProfileUseCaseFixture.NEW_EMAIL))
                .thenReturn(Optional.of(Duration.ofSeconds(7)));

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(TooManyAttemptsException.class);
        assertThat(((TooManyAttemptsException) thrown).retryAfterSeconds()).isEqualTo(7L);
    }

    @Test
    @DisplayName("Correio fora do ar propaga MailUnavailableException")
    void mailFailurePropagates() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        final var checker = currentChangeEmail(profile.id(), null);
        ready(profile);
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.CHANGE_EMAIL)).thenReturn(Optional.of(checker));
        when(profiles.findByEmail(ProfileUseCaseFixture.NEW_EMAIL)).thenReturn(Optional.empty());
        when(checkers.save(any(Checker.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new MailUnavailableException()).when(mailer).send(any());

        final var thrown = catchThrowable(() -> useCase.execute(command()));

        assertThat(thrown).isInstanceOf(MailUnavailableException.class);
    }

    private void ready(final com.sajitar.backend.domain.model.profile.Profile profile) {
        when(profiles.findById(profile.id())).thenReturn(Optional.of(profile));
        when(attempts.register(AttemptScope.CREDENTIALS, ProfileUseCaseFixture.ADDRESS)).thenReturn(Optional.empty());
        when(attempts.register(AttemptScope.CREDENTIALS, profile.email())).thenReturn(Optional.empty());
        when(attempts.register(AttemptScope.CREDENTIALS, ProfileUseCaseFixture.NEW_EMAIL)).thenReturn(Optional.empty());
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL)).thenReturn(Optional.empty());
    }

    private static Checker currentChangeEmail(final UUID profileId, final String payload) {
        return new Checker(
                Checker.uuidV7At(NOW.minus(Duration.ofHours(1))),
                profileId,
                Checker.Type.CHANGE_EMAIL,
                CODE,
                payload);
    }

    private static ConfirmChangeEmailCommand command() {
        return new ConfirmChangeEmailCommand(
                ProfileUseCaseFixture.ID,
                CODE,
                ProfileUseCaseFixture.NEW_EMAIL,
                ProfileUseCaseFixture.ADDRESS);
    }

}
