package com.sajitar.backend.adapter.out.mail;

import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.sajitar.backend.configuration.MailProperties;
import com.sajitar.backend.domain.exception.MailUnavailableException;
import com.sajitar.backend.domain.model.mail.MailMessage;
import com.sajitar.backend.domain.port.Mailer;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;

@Component
@Profile("LOCAL")
@RequiredArgsConstructor
class MailpitMailer implements Mailer {

    private final JavaMailSender mailSender;

    private final MailProperties properties;

    @Override
    public void send(final MailMessage message) {
        try {
            final var mime = mailSender.createMimeMessage();
            final var helper = new MimeMessageHelper(mime, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.from());
            helper.setTo(message.to());
            helper.setSubject(message.subject());
            helper.setText(message.body());
            mailSender.send(mime);
        } catch (final MessagingException | MailException _) {
            throw new MailUnavailableException();
        }
    }

}
