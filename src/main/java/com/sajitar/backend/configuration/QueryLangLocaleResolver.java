package com.sajitar.backend.configuration;

import java.util.Locale;
import java.util.Set;

import org.springframework.web.servlet.LocaleResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

final class QueryLangLocaleResolver implements LocaleResolver {

    static final String LANG_PARAMETER = "lang";

    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("en", "pt", "es");

    @Override
    public Locale resolveLocale(final HttpServletRequest request) {
        final var raw = request.getParameter(LANG_PARAMETER);
        if (raw == null || raw.isBlank()) {
            return Locale.ENGLISH;
        }
        final var language = Locale.forLanguageTag(raw.trim().replace('_', '-'))
                .getLanguage()
                .toLowerCase(Locale.ROOT);
        if (SUPPORTED_LANGUAGES.contains(language)) {
            return Locale.of(language);
        }
        return Locale.ENGLISH;
    }

    @Override
    public void setLocale(final HttpServletRequest request, final HttpServletResponse response, final Locale locale) {
    }

}
