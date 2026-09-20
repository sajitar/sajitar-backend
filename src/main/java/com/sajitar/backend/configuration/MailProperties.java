package com.sajitar.backend.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sajitar.mail")
public record MailProperties(String host, int port, String from) {

    static final int MIN_PORT = 1;

    static final int MAX_PORT = 65535;

    public MailProperties {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("sajitar.mail.host must not be blank");
        }
        if (port < MIN_PORT || port > MAX_PORT) {
            throw new IllegalArgumentException("sajitar.mail.port must be between 1 and 65535");
        }
        if (from == null || from.isBlank()) {
            throw new IllegalArgumentException("sajitar.mail.from must not be blank");
        }
    }

}
