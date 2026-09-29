package com.sajitar.backend.adapter.out.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import com.sajitar.backend.configuration.MailPropertiesFixture;
import com.sajitar.backend.domain.exception.MailUnavailableException;
import com.sajitar.backend.domain.model.mail.MailMessage;

import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;

@ExtendWith(OutputCaptureExtension.class)
@DisplayName("MailpitMailer")
class MailpitMailerTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);

    private final MailpitMailer mailer = new MailpitMailer(mailSender, MailPropertiesFixture.defaults());

    @BeforeEach
    void captureErrorLog() {
        routeErrorLogToStdout(MailpitMailer.class);
    }

    @Test
    @DisplayName("Envia HTML com remetente da configuração")
    void sendsHtmlWithConfiguredFrom() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        final var message = new MailMessage("alice@example.com", "Assunto", "<p>Corpo</p>");

        mailer.send(message);

        final var captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        final var sent = captor.getValue();
        assertThat(sent.getFrom()[0].toString()).contains(MailPropertiesFixture.FROM);
        assertThat(sent.getAllRecipients()[0].toString()).contains("alice@example.com");
        assertThat(sent.getSubject()).isEqualTo("Assunto");
        assertThat(sent.getDataHandler().getContentType()).contains("html");
        assertThat(sent.getContent().toString()).contains("<p>Corpo</p>");
    }

    @Test
    @DisplayName("Falha SMTP vira MailUnavailableException")
    void wrapsMailException(final CapturedOutput output) {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(MimeMessage.class));

        final var thrown = catchThrowable(() -> mailer.send(
                new MailMessage("alice@example.com", "Assunto", "Corpo")));

        assertThat(thrown).isInstanceOf(MailUnavailableException.class);
        assertThat(output).contains("Mail unavailable", "smtp down");
    }

    @Test
    @DisplayName("Falha ao montar a mensagem vira MailUnavailableException")
    void wrapsMessagingException(final CapturedOutput output) throws Exception {
        final var mime = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mime);
        doThrow(new MessagingException("invalid from")).when(mime).setFrom(any(Address.class));

        final var thrown = catchThrowable(() -> mailer.send(
                new MailMessage("alice@example.com", "Assunto", "Corpo")));

        assertThat(thrown).isInstanceOf(MailUnavailableException.class);
        assertThat(output).contains("Mail unavailable", "invalid from");
    }

    private static void routeErrorLogToStdout(final Class<?> type) {
        if (!(org.slf4j.LoggerFactory.getILoggerFactory() instanceof LoggerContext context)) {
            return;
        }
        final var logger = context.getLogger(type);
        logger.detachAndStopAllAppenders();
        final var encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern("%msg%n%ex");
        encoder.start();
        final var appender = new ConsoleAppender<ILoggingEvent>();
        appender.setContext(context);
        appender.setEncoder(encoder);
        appender.setTarget("System.out");
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.ERROR);
        logger.setAdditive(false);
    }

}
