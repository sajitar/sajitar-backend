package com.sajitar.backend.application;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MailLayout {

    static final String RESOURCE = "/mail/message.html";

    private static final DateTimeFormatter SUBJECT_WHEN = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'")
            .withZone(ZoneOffset.UTC);

    private static final String TEMPLATE = read(MailLayout.class.getResourceAsStream(RESOURCE));

    public static String subjectWhen(final Instant instant) {
        return SUBJECT_WHEN.format(instant);
    }

    public static String render(
            final String lang,
            final String title,
            final String preheader,
            final String heading,
            final String lead,
            final String code,
            final String hint,
            final String footer) {
        return TEMPLATE
                .replace("{{lang}}", escape(lang == null || lang.isBlank() ? "en" : lang))
                .replace("{{title}}", escape(title))
                .replace("{{preheader}}", escape(preheader))
                .replace("{{heading}}", escape(heading))
                .replace("{{lead}}", escape(lead))
                .replace("{{code}}", displayCode(code))
                .replace("{{hint}}", escape(hint))
                .replace("{{footer}}", escape(footer));
    }

    static String read(final InputStream stream) {
        if (stream == null) {
            throw new IllegalStateException("mail/message.html is missing");
        }
        try (stream) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (final IOException exception) {
            throw new IllegalStateException("mail/message.html is unreadable", exception);
        }
    }

    static String displayCode(final String code) {
        final var digits = escape(code);
        if (digits.length() == 6) {
            return digits.substring(0, 3) + "&nbsp;" + digits.substring(3);
        }
        return digits;
    }

    static String escape(final String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

}
