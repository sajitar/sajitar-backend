package com.sajitar.backend.domain.exception;

import java.util.List;
import java.util.Map;

public final class InvalidRefreshTokenException extends DomainException {

    public static final String MESSAGE_KEY = "validation.refresh-token.invalid";

    public InvalidRefreshTokenException() {
        super(Map.of("refreshToken", List.of(MESSAGE_KEY)));
    }

}
