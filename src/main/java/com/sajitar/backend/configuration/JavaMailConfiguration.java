package com.sajitar.backend.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
@Profile("PROD")
@EnableConfigurationProperties(MailProperties.class)
class JavaMailConfiguration {

    @Bean
    JavaMailSender javaMailSender(final MailProperties properties) {
        final var sender = new JavaMailSenderImpl();
        sender.setHost(properties.host());
        sender.setPort(properties.port());
        final var mail = sender.getJavaMailProperties();
        mail.put("mail.smtp.connectiontimeout", "5000");
        mail.put("mail.smtp.timeout", "3000");
        mail.put("mail.smtp.writetimeout", "5000");
        if (properties.smtpAuth()) {
            sender.setUsername(properties.username());
            sender.setPassword(properties.password());
            mail.put("mail.smtp.auth", "true");
            mail.put("mail.smtp.starttls.enable", Boolean.toString(properties.startTls()));
        } else {
            mail.put("mail.smtp.auth", "false");
            mail.put("mail.smtp.starttls.enable", "false");
        }
        return sender;
    }

}
