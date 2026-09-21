package com.sajitar.backend.domain.exception;

import java.util.List;
import java.util.Map;

public final class InvalidCheckerVerificationException extends DomainException {

    public static final String EMAIL_KEY = "validation.checker.verify-email.email-invalid";

    public static final String CODE_KEY = "validation.checker.verify-email.code-invalid";

    private InvalidCheckerVerificationException(final String key) {
        super(Map.of(field(key), List.of(key)));
    }

    public static InvalidCheckerVerificationException forEmail() {
        return new InvalidCheckerVerificationException(EMAIL_KEY);
    }

    public static InvalidCheckerVerificationException forCode() {
        return new InvalidCheckerVerificationException(CODE_KEY);
    }

    private static String field(final String key) {
        return key.equals(EMAIL_KEY) ? "email" : "code";
    }

}
