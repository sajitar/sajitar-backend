package com.sajitar.backend.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;

import com.sajitar.backend.configuration.LocaleConfiguration;

@DisplayName("BearerAuthenticationEntryPoint")
class BearerAuthenticationEntryPointTest {

    private final BearerAuthenticationEntryPoint entryPoint = new BearerAuthenticationEntryPoint(
            new LocaleConfiguration().messageSource(),
            new LocaleConfiguration().localeResolver());

    static Stream<Arguments> unauthorizedMessages() {
        return Stream.of(
                Arguments.of(null, "must be a valid bearer token"),
                Arguments.of("pt", "deve ser um bearer token válido"),
                Arguments.of("es", "debe ser un bearer token válido"));
    }

    @ParameterizedTest(name = "lang={0}")
    @MethodSource("unauthorizedMessages")
    @DisplayName("401 JSON traduz a chave de token inválido")
    void writesUnauthorizedJsonFollowingLang(final String lang, final String expected) throws Exception {
        final var request = new MockHttpServletRequest();
        if (lang != null) {
            request.setParameter("lang", lang);
        }
        final var response = new MockHttpServletResponse();

        entryPoint.commence(request, response, mock(AuthenticationException.class));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).contains("json");
        assertThat(response.getContentAsString()).isEqualTo("{\"token\":[\"" + expected + "\"]}");
    }

}
