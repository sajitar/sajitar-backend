package com.sajitar.backend.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

import com.sajitar.backend.configuration.LocaleConfiguration;

@DisplayName("SignInMail")
class SignInMailTest {

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    @DisplayName("Compõe HTML com assunto datado e código só no corpo")
    void composesHtmlWithTimestampedSubject() {
        final var sentAt = Instant.parse("2026-09-20T21:28:03Z");
        final var message = SignInMail.compose(
                new LocaleConfiguration().messageSource(),
                sentAt,
                "user@example.com",
                "123456",
                30);

        assertThat(message.to()).isEqualTo("user@example.com");
        assertThat(message.subject()).isEqualTo("Your Sajitar code · 2026-09-20 21:28:03 UTC");
        assertThat(message.subject()).doesNotContain("123456");
        assertThat(message.body()).contains("<!DOCTYPE html");
        assertThat(message.body()).contains("<title>Your Sajitar code · 2026-09-20 21:28:03 UTC</title>");
        assertThat(message.body()).contains("123&nbsp;456");
        assertThat(message.body()).contains("Your sign-in code is 123456.");
        assertThat(message.body()).contains(
                "You have 30 minutes from the first request; after that the code expires and you must start again.");
    }

    @Test
    @DisplayName("Copy do e-mail respeita o locale atual")
    void followsCurrentLocale() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("pt"));
        final var message = SignInMail.compose(
                new LocaleConfiguration().messageSource(),
                Instant.parse("2026-09-20T21:28:03Z"),
                "user@example.com",
                "654321",
                30);

        assertThat(message.subject()).isEqualTo("Seu código Sajitar · 2026-09-20 21:28:03 UTC");
        assertThat(message.body()).contains("lang=\"pt\"");
        assertThat(message.body()).contains("Seu código de acesso é 654321.");
        assertThat(message.body()).contains("Você tem até 30 minutos, contados a partir do primeiro pedido");
    }

}
