package com.sajitar.backend.adapter.out.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import com.sajitar.backend.configuration.MailPropertiesFixture;
import com.sajitar.backend.domain.exception.MailUnavailableException;
import com.sajitar.backend.domain.model.mail.MailMessage;

import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

@DisplayName("MailpitMailer")
class MailpitMailerTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);

    private final MailpitMailer mailer = new MailpitMailer(mailSender, MailPropertiesFixture.defaults());

    @Test
    @DisplayName("Envia texto simples com remetente da configuração")
    void sendsPlainTextWithConfiguredFrom() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        final var message = new MailMessage("alice@example.com", "Assunto", "Corpo");

        mailer.send(message);

        final var captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        final var sent = captor.getValue();
        assertThat(sent.getFrom()[0].toString()).contains(MailPropertiesFixture.FROM);
        assertThat(sent.getAllRecipients()[0].toString()).contains("alice@example.com");
        assertThat(sent.getSubject()).isEqualTo("Assunto");
        assertThat(sent.getContent().toString()).contains("Corpo");
    }

    @Test
    @DisplayName("Falha SMTP vira MailUnavailableException")
    void wrapsMailException() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(MimeMessage.class));

        final var thrown = catchThrowable(() -> mailer.send(
                new MailMessage("alice@example.com", "Assunto", "Corpo")));

        assertThat(thrown).isInstanceOf(MailUnavailableException.class);
    }

    @Test
    @DisplayName("Falha ao montar a mensagem vira MailUnavailableException")
    void wrapsMessagingException() throws Exception {
        final var mime = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mime);
        doThrow(new MessagingException("invalid from")).when(mime).setFrom(any(Address.class));

        final var thrown = catchThrowable(() -> mailer.send(
                new MailMessage("alice@example.com", "Assunto", "Corpo")));

        assertThat(thrown).isInstanceOf(MailUnavailableException.class);
    }

}
