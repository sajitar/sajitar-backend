package com.sajitar.backend.domain.exception;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public final class TooManyAttemptsException extends DomainException {

    public static final String MESSAGE_KEY = "validation.attempts.too-many";

    private final Duration retryAfter;

    private TooManyAttemptsException(final String field, final Duration retryAfter) {
        super(Map.of(field, List.of(MESSAGE_KEY)));
        this.retryAfter = retryAfter;
    }

    public static TooManyAttemptsException forCredentials(final Duration retryAfter) {
        return new TooManyAttemptsException("credentials", retryAfter);
    }

    public static TooManyAttemptsException forRefreshToken(final Duration retryAfter) {
        return new TooManyAttemptsException("refreshToken", retryAfter);
    }

    public long retryAfterSeconds() {
        final var seconds = (retryAfter.toMillis() + 999L) / 1000L;
        return Math.max(1L, seconds);
    }

}
