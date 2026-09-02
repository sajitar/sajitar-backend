package com.sajitar.backend.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.sajitar.backend.configuration.LocaleConfiguration;
import com.sajitar.backend.domain.exception.EmailAlreadyRegisteredException;
import com.sajitar.backend.domain.exception.ProfileNotFoundException;
import com.sajitar.backend.domain.validation.profile.Name;

import jakarta.validation.ConstraintViolationException;

@DisplayName("WebExceptionHandler")
class WebExceptionHandlerTest {

    private WebExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new WebExceptionHandler(new LocaleConfiguration().messageSource());
    }

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    static Stream<Arguments> conflictMessages() {
        return Stream.of(
                Arguments.of("en", "must be an unregistered email"),
                Arguments.of("pt", "deve ser um e-mail não registrado"),
                Arguments.of("es", "debe ser un correo no registrado"));
    }

    static Stream<Arguments> typeMismatchMessages() {
        return Stream.of(
                Arguments.of("en", "must belong to type UUID"),
                Arguments.of("pt", "deve pertencer ao tipo UUID"),
                Arguments.of("es", "debe pertenecer al tipo UUID"));
    }

    @ParameterizedTest(name = "lang={0}")
    @MethodSource("conflictMessages")
    @DisplayName("409 traduz a chave de e-mail já registrado")
    void conflictFollowsLocale(final String lang, final String expected) {
        LocaleContextHolder.setLocale(Locale.forLanguageTag(lang));

        final var response = handler.handle(new EmailAlreadyRegisteredException());

        assertThat(response.getStatusCode()).isEqualTo(CONFLICT);
        assertThat(response.getBody()).containsOnlyKeys("email");
        assertThat(response.getBody().get("email")).containsExactly(expected);
    }

    @ParameterizedTest(name = "lang={0}")
    @MethodSource("typeMismatchMessages")
    @DisplayName("400 de tipo inválido traduz o nome do tipo")
    void typeMismatchFollowsLocale(final String lang, final String expected) {
        LocaleContextHolder.setLocale(Locale.forLanguageTag(lang));
        final var exception = new MethodArgumentTypeMismatchException(
                "bad",
                UUID.class,
                "id",
                mock(MethodParameter.class),
                new IllegalArgumentException());

        final var response = handler.handle(exception);

        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
        assertThat(response.getBody()).containsOnlyKeys("id");
        assertThat(response.getBody().get("id")).containsExactly(expected);
    }

    @Test
    @DisplayName("400 de tipo inválido usa unknown quando o tipo requerido é nulo")
    void typeMismatchWithoutRequiredType() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        final var exception = new MethodArgumentTypeMismatchException(
                "bad",
                null,
                "id",
                mock(MethodParameter.class),
                new IllegalArgumentException());

        final var response = handler.handle(exception);

        assertThat(response.getBody().get("id")).containsExactly("must belong to type unknown");
    }

    @Test
    @DisplayName("404 de perfil inexistente não tem corpo")
    void profileNotFoundHasEmptyBody() {
        final var response = handler.handle(new ProfileNotFoundException());

        assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    @DisplayName("400 de ConstraintViolationException agrupa mensagens já interpoladas")
    void constraintViolationKeepsInterpolatedMessage() {
        final var thrown = catchThrowable(() -> Name.Validation.validate("123"));
        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);

        final var response = handler.handle((ConstraintViolationException) thrown);

        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
        assertThat(response.getBody()).containsOnlyKeys("name");
        assertThat(response.getBody().get("name")).containsExactly("must be a well-formed name");
    }

    @Test
    @DisplayName("400 de MethodArgumentNotValidException usa a mensagem do FieldError")
    void methodArgumentNotValidUsesFieldErrorMessage() throws Exception {
        final var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "name", "must be a well-formed name"));
        final var exception = new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);

        final var response = handler.handle(exception);

        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
        assertThat(response.getBody()).containsOnlyKeys("name");
        assertThat(response.getBody().get("name")).containsExactly("must be a well-formed name");
    }

}
