package com.sajitar.backend.configuration;

import java.nio.charset.StandardCharsets;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sajitar.security.jwt")
public record JwtProperties(
        String secret,
        int expirationSeconds,
        int refreshExpirationSeconds,
        int sessionMaxSeconds,
        int maxSessionsPerProfile,
        int refreshGraceSeconds,
        String issuer,
        String audience) {

    static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException("sajitar.security.jwt.secret must contain at least 32 bytes");
        }
        if (expirationSeconds <= 0) {
            throw new IllegalArgumentException("sajitar.security.jwt.expiration-seconds must be greater than 0");
        }
        if (refreshExpirationSeconds <= 0) {
            throw new IllegalArgumentException(
                    "sajitar.security.jwt.refresh-expiration-seconds must be greater than 0");
        }
        if (refreshExpirationSeconds <= expirationSeconds) {
            throw new IllegalArgumentException(
                    "sajitar.security.jwt.refresh-expiration-seconds must be greater than expiration-seconds");
        }
        if (sessionMaxSeconds <= refreshExpirationSeconds) {
            throw new IllegalArgumentException(
                    "sajitar.security.jwt.session-max-seconds must be greater than refresh-expiration-seconds");
        }
        if (maxSessionsPerProfile <= 0) {
            throw new IllegalArgumentException(
                    "sajitar.security.jwt.max-sessions-per-profile must be greater than 0");
        }
        if (refreshGraceSeconds < 0) {
            throw new IllegalArgumentException("sajitar.security.jwt.refresh-grace-seconds must not be negative");
        }
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("sajitar.security.jwt.issuer must not be blank");
        }
        if (audience == null || audience.isBlank()) {
            throw new IllegalArgumentException("sajitar.security.jwt.audience must not be blank");
        }
    }

}
