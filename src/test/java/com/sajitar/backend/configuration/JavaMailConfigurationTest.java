package com.sajitar.backend.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@DisplayName("JavaMailConfiguration")
class JavaMailConfigurationTest {

    @Test
    @DisplayName("Sem username: SMTP sem autenticação nem STARTTLS")
    void buildsSenderWithoutAuth() {
        final var sender = new JavaMailConfiguration().javaMailSender(MailPropertiesFixture.defaults());

        assertThat(sender).isInstanceOf(JavaMailSenderImpl.class);
        final var impl = (JavaMailSenderImpl) sender;
        assertThat(impl.getHost()).isEqualTo(MailPropertiesFixture.HOST);
        assertThat(impl.getPort()).isEqualTo(MailPropertiesFixture.PORT);
        assertThat(impl.getUsername()).isNull();
        assertThat(impl.getJavaMailProperties())
                .containsEntry("mail.smtp.auth", "false")
                .containsEntry("mail.smtp.starttls.enable", "false")
                .containsEntry("mail.smtp.connectiontimeout", "5000")
                .containsEntry("mail.smtp.timeout", "3000")
                .containsEntry("mail.smtp.writetimeout", "5000");
    }

    @Test
    @DisplayName("Com username: autenticação e STARTTLS conforme as propriedades")
    void buildsSenderWithAuthAndStartTls() {
        final var properties = new MailProperties(MailPropertiesFixture.HOST, MailPropertiesFixture.PORT,
                MailPropertiesFixture.FROM, "smtp-user", "smtp-pass", true);

        final var impl = (JavaMailSenderImpl) new JavaMailConfiguration().javaMailSender(properties);

        assertThat(impl.getUsername()).isEqualTo("smtp-user");
        assertThat(impl.getPassword()).isEqualTo("smtp-pass");
        assertThat(impl.getJavaMailProperties())
                .containsEntry("mail.smtp.auth", "true")
                .containsEntry("mail.smtp.starttls.enable", "true");
    }

    @Test
    @DisplayName("Com username e STARTTLS desligado: auth sem STARTTLS")
    void buildsSenderWithAuthWithoutStartTls() {
        final var properties = new MailProperties(MailPropertiesFixture.HOST, MailPropertiesFixture.PORT,
                MailPropertiesFixture.FROM, "smtp-user", "smtp-pass", false);

        final var impl = (JavaMailSenderImpl) new JavaMailConfiguration().javaMailSender(properties);

        assertThat(impl.getJavaMailProperties())
                .containsEntry("mail.smtp.auth", "true")
                .containsEntry("mail.smtp.starttls.enable", "false");
    }

}
