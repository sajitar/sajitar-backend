package com.sajitar.backend.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@DisplayName("QueryLangLocaleResolver")
class QueryLangLocaleResolverTest {

    private final QueryLangLocaleResolver resolver = new QueryLangLocaleResolver();

    static Stream<Arguments> localeArguments() {
        return Stream.of(
                Arguments.of(null, "en"),
                Arguments.of("", "en"),
                Arguments.of("   ", "en"),
                Arguments.of("en", "en"),
                Arguments.of("EN", "en"),
                Arguments.of("en-US", "en"),
                Arguments.of("PT", "pt"),
                Arguments.of("pt-BR", "pt"),
                Arguments.of("pt_BR", "pt"),
                Arguments.of("  pt  ", "pt"),
                Arguments.of("es", "es"),
                Arguments.of("es-MX", "es"),
                Arguments.of("fr", "en"),
                Arguments.of("zh-CN", "en"));
    }

    @ParameterizedTest(name = "lang={0} → {1}")
    @MethodSource("localeArguments")
    @DisplayName("Resolve idioma pela query lang, com inglês como padrão")
    void resolvesLanguageFromQuery(final String lang, final String expectedLanguage) {
        final var request = mock(HttpServletRequest.class);
        when(request.getParameter(QueryLangLocaleResolver.LANG_PARAMETER)).thenReturn(lang);

        final var locale = resolver.resolveLocale(request);

        assertThat(locale.getLanguage()).isEqualTo(expectedLanguage);
    }

    @Test
    @DisplayName("setLocale é no-op porque cada request resolve lang de forma independente")
    void setLocaleDoesNotChangeSubsequentResolution() {
        final var request = mock(HttpServletRequest.class);
        when(request.getParameter(QueryLangLocaleResolver.LANG_PARAMETER)).thenReturn(null);

        resolver.setLocale(request, mock(HttpServletResponse.class), Locale.FRENCH);

        assertThat(resolver.resolveLocale(request).getLanguage()).isEqualTo("en");
    }

}
