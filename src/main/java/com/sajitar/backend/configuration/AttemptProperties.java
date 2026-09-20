package com.sajitar.backend.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sajitar.security.attempt")
public record AttemptProperties(
        int credentialsMax,
        int credentialsWindowSeconds,
        int refreshMax,
        int refreshWindowSeconds,
        boolean trustForwardedFor) {

    public AttemptProperties {
        if (credentialsMax <= 0) {
            throw new IllegalArgumentException(
                    "sajitar.security.attempt.credentials-max must be greater than 0");
        }
        if (credentialsWindowSeconds <= 0) {
            throw new IllegalArgumentException(
                    "sajitar.security.attempt.credentials-window-seconds must be greater than 0");
        }
        if (refreshMax <= 0) {
            throw new IllegalArgumentException("sajitar.security.attempt.refresh-max must be greater than 0");
        }
        if (refreshWindowSeconds <= 0) {
            throw new IllegalArgumentException(
                    "sajitar.security.attempt.refresh-window-seconds must be greater than 0");
        }
    }

}
