package com.sajitar.backend.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@DisplayName("MailpitMailConfiguration")
class MailpitMailConfigurationTest {

    @Test
    @DisplayName("Expõe JavaMailSender SMTP sem autenticação nem STARTTLS, com timeouts finitos")
    void buildsMailpitSender() {
        final var properties = MailPropertiesFixture.defaults();

        final var sender = new MailpitMailConfiguration().mailpitJavaMailSender(properties);

        assertThat(sender).isInstanceOf(JavaMailSenderImpl.class);
        final var impl = (JavaMailSenderImpl) sender;
        assertThat(impl.getHost()).isEqualTo(MailPropertiesFixture.HOST);
        assertThat(impl.getPort()).isEqualTo(MailPropertiesFixture.PORT);
        assertThat(impl.getJavaMailProperties())
                .containsEntry("mail.smtp.auth", "false")
                .containsEntry("mail.smtp.starttls.enable", "false")
                .containsEntry("mail.smtp.connectiontimeout", "5000")
                .containsEntry("mail.smtp.timeout", "3000")
                .containsEntry("mail.smtp.writetimeout", "5000");
    }

}
