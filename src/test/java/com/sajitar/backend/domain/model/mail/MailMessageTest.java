package com.sajitar.backend.domain.model.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("MailMessage")
class MailMessageTest {

    @Test
    @DisplayName("Aceita destinatário, assunto e corpo preenchidos")
    void acceptsValidMessage() {
        final var message = new MailMessage("alice@example.com", "Assunto", "Corpo");

        assertThat(message.to()).isEqualTo("alice@example.com");
        assertThat(message.subject()).isEqualTo("Assunto");
        assertThat(message.body()).isEqualTo("Corpo");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   " })
    @DisplayName("Rejeita destinatário em branco")
    void rejectsBlankTo(final String to) {
        final var thrown = catchThrowable(() -> new MailMessage(to, "Assunto", "Corpo"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("to");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   " })
    @DisplayName("Rejeita assunto em branco")
    void rejectsBlankSubject(final String subject) {
        final var thrown = catchThrowable(() -> new MailMessage("alice@example.com", subject, "Corpo"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("subject");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   " })
    @DisplayName("Rejeita corpo em branco")
    void rejectsBlankBody(final String body) {
        final var thrown = catchThrowable(() -> new MailMessage("alice@example.com", "Assunto", body));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("body");
    }

}
