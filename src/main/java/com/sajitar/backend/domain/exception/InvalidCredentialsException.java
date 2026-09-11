package com.sajitar.backend.domain.exception;

import java.util.List;
import java.util.Map;

public final class InvalidCredentialsException extends DomainException {

    public static final String MESSAGE_KEY = "validation.credentials.invalid";

    public InvalidCredentialsException() {
        super(Map.of("credentials", List.of(MESSAGE_KEY)));
    }

}
