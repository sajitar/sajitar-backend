package com.sajitar.backend.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "sajitar.mail")
public record MailProperties(String host, int port, String from, String username, String password, boolean startTls) {

    static final int MIN_PORT = 1;

    static final int MAX_PORT = 65535;

    public MailProperties(final String host, final int port, final String from) {
        this(host, port, from, "", "", false);
    }

    @ConstructorBinding
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
        if (username == null || username.isBlank()) {
            username = "";
            password = password == null ? "" : password;
            startTls = false;
        } else {
            password = password == null ? "" : password;
        }
    }

    public boolean smtpAuth() {
        return !username.isBlank();
    }

}
