package com.sajitar.backend.application.usecase.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;

import com.sajitar.backend.application.command.profile.CreateProfileCommand;
import com.sajitar.backend.configuration.LocaleConfiguration;
import com.sajitar.backend.configuration.ProfilePurgeProperties;
import com.sajitar.backend.domain.exception.EmailAlreadyRegisteredException;
import com.sajitar.backend.domain.exception.MailUnavailableException;
import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.model.mail.MailMessage;
import com.sajitar.backend.domain.model.profile.Profile;
import com.sajitar.backend.domain.port.Mailer;
import com.sajitar.backend.domain.port.PasswordHasher;
import com.sajitar.backend.domain.port.checker.CheckerRepository;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.validation.Limit;
import com.sajitar.backend.domain.validation.profile.Birthday;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateProfileUseCase")
class CreateProfileUseCaseTest {

    private static final Instant SENT_AT = Instant.parse("2026-09-20T21:28:03Z");

    private static final Clock CLOCK = Clock.fixed(SENT_AT, ZoneOffset.UTC);

    private static final String SENT_AT_SUBJECT = "2026-09-20 21:28:03 UTC";

    @Mock
    private ProfileRepository profiles;

    @Mock
    private CheckerRepository checkers;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private Mailer mailer;

    private CreateProfileUseCase useCase;

    @BeforeAll
    static void configureValidation() {
        Birthday.BirthdayValidator.configure(18);
        Limit.LimitValidator.configure(100);
    }

    @BeforeEach
    void setUp() {
        useCase = new CreateProfileUseCase(
                profiles,
                checkers,
                passwordHasher,
                mailer,
                CLOCK,
                new ProfilePurgeProperties(48, "UTC"),
                new LocaleConfiguration().messageSource(),
                ProfileUseCaseFixture.VALIDATOR);
    }

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    @DisplayName("Codifica a senha, persiste VERIFY_EMAIL e envia o código por e-mail")
    void hashesPasswordPersistsVerifyEmailAndSendsMail() {
        final var command = ProfileUseCaseFixture.validCreateCommand();
        when(profiles.findByEmail(command.email())).thenReturn(Optional.empty());
        when(passwordHasher.hash(command.password())).thenReturn("$2a$encoded");
        when(profiles.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(checkers.save(any(Checker.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command);

        assertThat(saved.password()).isEqualTo("$2a$encoded");
        assertThat(saved.email()).isEqualTo(command.email());
        assertThat(saved.id()).isNotNull();
        verify(passwordHasher).hash(command.password());
        final var profileCaptor = ArgumentCaptor.forClass(Profile.class);
        verify(profiles).save(profileCaptor.capture());
        assertThat(profileCaptor.getValue().password()).isEqualTo("$2a$encoded");
        verify(profiles).findByEmail(command.email());
        verifyNoMoreInteractions(profiles);
        final var checkerCaptor = ArgumentCaptor.forClass(Checker.class);
        verify(checkers).save(checkerCaptor.capture());
        final var checker = checkerCaptor.getValue();
        assertThat(checker.profileId()).isEqualTo(saved.id());
        assertThat(checker.type()).isEqualTo(Checker.Type.VERIFY_EMAIL);
        assertThat(checker.code()).matches("^[0-9]{6}$");
        assertThat(checker.payload()).isNull();
        final var mailCaptor = ArgumentCaptor.forClass(MailMessage.class);
        verify(mailer).send(mailCaptor.capture());
        final var mail = mailCaptor.getValue();
        assertThat(mail.to()).isEqualTo(command.email());
        assertThat(mail.subject()).isEqualTo("Your Sajitar code · " + SENT_AT_SUBJECT);
        assertThat(mail.subject()).doesNotContain(checker.code());
        assertThat(mail.body()).contains("<!DOCTYPE html");
        assertThat(mail.body()).contains("<title>Your Sajitar code · " + SENT_AT_SUBJECT + "</title>");
        assertThat(mail.body()).contains("lang=\"en\"");
        assertThat(mail.body()).contains("Your verification code is " + checker.code() + ".");
        assertThat(mail.body()).contains("One step to activate your account");
        assertThat(mail.body()).contains("You have 48 hours from account creation");
        assertThat(mail.body()).contains(
                checker.code().substring(0, 3) + "&nbsp;" + checker.code().substring(3));
    }

    @Test
    @DisplayName("Assunto e corpo do e-mail respeitam o locale atual")
    void mailFollowsCurrentLocale() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("pt"));
        final var command = ProfileUseCaseFixture.validCreateCommand();
        when(profiles.findByEmail(command.email())).thenReturn(Optional.empty());
        when(passwordHasher.hash(command.password())).thenReturn("$2a$encoded");
        when(profiles.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(checkers.save(any(Checker.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(command);

        final var mailCaptor = ArgumentCaptor.forClass(MailMessage.class);
        verify(mailer).send(mailCaptor.capture());
        final var mail = mailCaptor.getValue();
        assertThat(mail.subject()).isEqualTo("Seu código Sajitar · " + SENT_AT_SUBJECT);
        assertThat(mail.body()).contains("lang=\"pt\"");
        assertThat(mail.body()).contains("Seu código de verificação é ");
        assertThat(mail.body()).contains("Falta um passo para ativar sua conta");
        assertThat(mail.body()).contains("Você tem até 48 horas, contadas a partir da criação da conta");
    }

    @Test
    @DisplayName("Assunto e corpo do e-mail respeitam o locale espanhol")
    void mailFollowsSpanishLocale() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("es"));
        final var command = ProfileUseCaseFixture.validCreateCommand();
        when(profiles.findByEmail(command.email())).thenReturn(Optional.empty());
        when(passwordHasher.hash(command.password())).thenReturn("$2a$encoded");
        when(profiles.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(checkers.save(any(Checker.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(command);

        final var mailCaptor = ArgumentCaptor.forClass(MailMessage.class);
        verify(mailer).send(mailCaptor.capture());
        final var mail = mailCaptor.getValue();
        assertThat(mail.subject()).isEqualTo("Su código Sajitar · " + SENT_AT_SUBJECT);
        assertThat(mail.body()).contains("lang=\"es\"");
        assertThat(mail.body()).contains("Su código de verificación es ");
        assertThat(mail.body()).contains("Falta un paso para activar su cuenta");
        assertThat(mail.body()).contains("Tiene 48 horas desde la creación de la cuenta");
    }

    @Test
    @DisplayName("Falha de envio propaga MailUnavailableException")
    void mailFailurePropagates() {
        final var command = ProfileUseCaseFixture.validCreateCommand();
        when(profiles.findByEmail(command.email())).thenReturn(Optional.empty());
        when(passwordHasher.hash(command.password())).thenReturn("$2a$encoded");
        when(profiles.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(checkers.save(any(Checker.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new MailUnavailableException()).when(mailer).send(any(MailMessage.class));

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(MailUnavailableException.class);
    }

    @Test
    @DisplayName("Lança EmailAlreadyRegisteredException quando o e-mail já existe")
    void throwsWhenEmailIsAlreadyRegistered() {
        final var command = ProfileUseCaseFixture.validCreateCommand();
        when(profiles.findByEmail(command.email())).thenReturn(Optional.of(ProfileUseCaseFixture.persistedProfile()));

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(EmailAlreadyRegisteredException.class);
        final var ex = (EmailAlreadyRegisteredException) thrown;
        assertThat(ex.content()).containsKey("email");
        assertThat(ex.content().get("email")).containsExactly(EmailAlreadyRegisteredException.MESSAGE_KEY);
        verify(profiles).findByEmail(command.email());
        verify(passwordHasher, never()).hash(any());
        verify(profiles, never()).save(any());
        verify(checkers, never()).save(any());
        verify(mailer, never()).send(any());
    }

    @Test
    @DisplayName("Nome inválido: não consulta nem persiste")
    void doesNotTouchRepositoryWhenNameIsInvalid() {
        final var command = new CreateProfileCommand(
                "123",
                ProfileUseCaseFixture.DESCRIPTION,
                ProfileUseCaseFixture.BIRTHDAY,
                ProfileUseCaseFixture.EMAIL,
                ProfileUseCaseFixture.PASSWORD);

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(Pattern.class);
        assertThat(violation.getPropertyPath().toString()).isEqualTo("name");
        verify(profiles, never()).findByEmail(any());
        verify(profiles, never()).save(any());
        verify(checkers, never()).save(any());
        verify(mailer, never()).send(any());
    }

    @ParameterizedTest
    @ValueSource(strings = { "not-an-email", "User@Example.com" })
    @DisplayName("E-mail inválido: não consulta o repositório")
    void doesNotTouchRepositoryWhenEmailIsInvalid(final String email) {
        final var command = new CreateProfileCommand(
                ProfileUseCaseFixture.NAME,
                ProfileUseCaseFixture.DESCRIPTION,
                ProfileUseCaseFixture.BIRTHDAY,
                email,
                ProfileUseCaseFixture.PASSWORD);

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findByEmail(any());
        verify(profiles, never()).save(any());
        verify(checkers, never()).save(any());
        verify(mailer, never()).send(any());
    }

    @Test
    @DisplayName("Senha curta: não consulta nem hasheia")
    void doesNotTouchRepositoryWhenPasswordIsTooShort() {
        final var command = new CreateProfileCommand(
                ProfileUseCaseFixture.NAME,
                ProfileUseCaseFixture.DESCRIPTION,
                ProfileUseCaseFixture.BIRTHDAY,
                ProfileUseCaseFixture.EMAIL,
                "1234567");

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(Size.class);
        assertThat(violation.getPropertyPath().toString()).isEqualTo("password");
        verify(passwordHasher, never()).hash(any());
        verify(profiles, never()).save(any());
        verify(checkers, never()).save(any());
        verify(mailer, never()).send(any());
    }

    @Test
    @DisplayName("E-mail nulo: violação @NotNull no command")
    void rejectsNullEmail() {
        final var command = new CreateProfileCommand(
                ProfileUseCaseFixture.NAME,
                ProfileUseCaseFixture.DESCRIPTION,
                ProfileUseCaseFixture.BIRTHDAY,
                null,
                ProfileUseCaseFixture.PASSWORD);

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotNull.class);
        verify(profiles, never()).findByEmail(any());
        verify(checkers, never()).save(any());
        verify(mailer, never()).send(any());
    }

    @Test
    @DisplayName("Nascimento recente demais: não persiste")
    void rejectsBirthdayBelowMinAge() {
        final var command = new CreateProfileCommand(
                ProfileUseCaseFixture.NAME,
                ProfileUseCaseFixture.DESCRIPTION,
                LocalDate.now().minusYears(10),
                ProfileUseCaseFixture.EMAIL,
                ProfileUseCaseFixture.PASSWORD);

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).save(any());
        verify(checkers, never()).save(any());
        verify(mailer, never()).send(any());
    }

    @Nested
    @DisplayName("identidade")
    class Identity {

        @Test
        @DisplayName("Gera um id antes de persistir")
        void generatesIdBeforeSave() {
            when(profiles.findByEmail(any())).thenReturn(Optional.empty());
            when(passwordHasher.hash(any())).thenReturn("$2a$encoded");
            when(profiles.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(checkers.save(any(Checker.class))).thenAnswer(invocation -> invocation.getArgument(0));

            final var saved = useCase.execute(ProfileUseCaseFixture.validCreateCommand());

            assertThat(saved.id()).isNotNull();
            verify(profiles).save(eq(saved));
        }

        @Test
        @DisplayName("Perfis com o mesmo id são iguais independentemente dos demais campos")
        void equalsByIdOnly() {
            final var id = UUID.randomUUID();
            final var a = ProfileUseCaseFixture.persistedProfile().withId(id).withName("A");
            final var b = ProfileUseCaseFixture.persistedProfile().withId(id).withName("B");
            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }
    }

}
