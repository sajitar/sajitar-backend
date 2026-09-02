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
