package com.sajitar.backend.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.servlet.LocaleResolver;

import com.sajitar.backend.domain.validation.profile.Name;

import jakarta.validation.ConstraintViolationException;

@DisplayName("LocaleConfiguration")
class LocaleConfigurationTest {

    private final LocaleConfiguration configuration = new LocaleConfiguration();

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    @DisplayName("LocaleResolver é o resolver stateless da query lang")
    void localeResolverIsQueryLangResolver() {
        final LocaleResolver resolver = configuration.localeResolver();

        assertThat(resolver).isInstanceOf(QueryLangLocaleResolver.class);
    }

    @Test
    @DisplayName("MessageSource usa inglês como fallback quando o locale não tem bundle")
    void messageSourceFallsBackToEnglish() {
        final var source = configuration.messageSource();

        assertThat(source.getMessage("validation.name.pattern", null, Locale.ENGLISH))
                .isEqualTo("must be a well-formed name");
        assertThat(source.getMessage("validation.name.pattern", null, Locale.forLanguageTag("pt")))
                .isEqualTo("deve ser um nome bem formado");
        assertThat(source.getMessage("validation.name.pattern", null, Locale.forLanguageTag("es")))
                .isEqualTo("debe ser un nombre bien formado");
        assertThat(source.getMessage("validation.name.pattern", null, Locale.FRENCH))
                .isEqualTo("must be a well-formed name");
    }

    @Test
    @DisplayName("MessageSource resolve assunto e corpo do e-mail de verificação")
    void messageSourceResolvesVerifyEmailMail() {
        final var source = configuration.messageSource();

        assertThat(source.getMessage(
                "mail.verify-email.subject",
                new Object[] { "2026-09-20 21:28:03 UTC" },
                Locale.ENGLISH))
                .isEqualTo("Your Sajitar code · 2026-09-20 21:28:03 UTC");
        assertThat(source.getMessage("mail.verify-email.body", new Object[] { "123456" }, Locale.ENGLISH))
                .isEqualTo("Your verification code is 123456.");
        assertThat(source.getMessage(
                "mail.verify-email.subject",
                new Object[] { "2026-09-20 21:28:03 UTC" },
                Locale.forLanguageTag("pt")))
                .isEqualTo("Seu código Sajitar · 2026-09-20 21:28:03 UTC");
        assertThat(source.getMessage("mail.verify-email.body", new Object[] { "654321" }, Locale.forLanguageTag("pt")))
                .isEqualTo("Seu código de verificação é 654321.");
        assertThat(source.getMessage(
                "mail.verify-email.subject",
                new Object[] { "2026-09-20 21:28:03 UTC" },
                Locale.forLanguageTag("es")))
                .isEqualTo("Su código Sajitar · 2026-09-20 21:28:03 UTC");
        assertThat(source.getMessage("mail.verify-email.body", new Object[] { "111222" }, Locale.forLanguageTag("es")))
                .isEqualTo("Su código de verificación es 111222.");
        assertThat(source.getMessage("mail.verify-email.preheader", new Object[] { "123456" }, Locale.ENGLISH))
                .isEqualTo("Your Sajitar code is 123456");
        assertThat(source.getMessage("mail.verify-email.heading", null, Locale.ENGLISH))
                .isEqualTo("One step to activate your account");
        assertThat(source.getMessage("mail.verify-email.hint", new Object[] { 48 }, Locale.ENGLISH))
                .isEqualTo(
                        "Enter it with your password the first time you sign in. You have 48 hours from account creation to verify this address; after that the profile is deleted and you must start again from scratch.");
        assertThat(source.getMessage("mail.verify-email.hint", new Object[] { 48 }, Locale.forLanguageTag("pt")))
                .isEqualTo(
                        "Informe-o com a senha no primeiro acesso. Você tem até 48 horas, contadas a partir da criação da conta, para validar o e-mail; depois disso o perfil é apagado e o cadastro precisa recomeçar do zero.");
        assertThat(source.getMessage("mail.verify-email.hint", new Object[] { 48 }, Locale.forLanguageTag("es")))
                .isEqualTo(
                        "Introdúzcalo con la contraseña en el primer acceso. Tiene 48 horas desde la creación de la cuenta para validar el correo; después el perfil se elimina y deberá crear la cuenta desde cero.");
        assertThat(source.getMessage("mail.verify-email.footer", null, Locale.forLanguageTag("es")))
                .isEqualTo("Recibió esto porque se creó una cuenta Sajitar con esta dirección. No comparta el código.");
        assertThat(source.getMessage("validation.checker.verify-email.code-invalid", null, Locale.ENGLISH))
                .isEqualTo("must be a valid verification code");
        assertThat(source.getMessage("validation.checker.verify-email.code-invalid", null, Locale.forLanguageTag("pt")))
                .isEqualTo("deve ser um código de verificação válido");
    }

    @Test
    @DisplayName("Validator interpola o bundle do locale atual")
    void validatorInterpolatesCurrentLocale() {
        final var factory = configuration.validator(configuration.messageSource());
        factory.afterPropertiesSet();
        LocaleContextHolder.setLocale(Locale.forLanguageTag("pt"));

        final var thrown = catchThrowable(() -> Name.Validation.validate(factory.getValidator(), "123"));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var message = ((ConstraintViolationException) thrown).getConstraintViolations()
                .iterator()
                .next()
                .getMessage();
        assertThat(message).isEqualTo("deve ser um nome bem formado");
    }

}
