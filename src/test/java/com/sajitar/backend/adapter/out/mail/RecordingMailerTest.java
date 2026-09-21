package com.sajitar.backend.adapter.out.mail;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sajitar.backend.domain.model.mail.MailMessage;

@DisplayName("RecordingMailer")
class RecordingMailerTest {

    private final RecordingMailer mailer = new RecordingMailer();

    @Test
    @DisplayName("Registra mensagens enviadas e limpa a lista")
    void recordsAndClearsSentMessages() {
        mailer.send(new MailMessage("alice@example.com", "Assunto", "Corpo"));

        assertThat(mailer.sent()).hasSize(1);
        assertThat(mailer.sent().getFirst().to()).isEqualTo("alice@example.com");

        mailer.clear();

        assertThat(mailer.sent()).isEmpty();
    }

}
