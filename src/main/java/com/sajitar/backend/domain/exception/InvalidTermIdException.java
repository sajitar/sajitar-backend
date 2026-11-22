package com.sajitar.backend.domain.exception;

import java.util.List;
import java.util.Map;

public final class InvalidTermIdException extends DomainException {

    public static final String MESSAGE_KEY = "validation.termId.invalid";

    public InvalidTermIdException() {
        super(Map.of("termId", List.of(MESSAGE_KEY)));
    }

}
