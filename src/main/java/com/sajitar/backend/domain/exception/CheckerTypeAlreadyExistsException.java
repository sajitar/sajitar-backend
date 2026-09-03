package com.sajitar.backend.domain.exception;

import java.util.List;
import java.util.Map;

public final class CheckerTypeAlreadyExistsException extends DomainException {

    public static final String MESSAGE_KEY = "validation.checker.type.already-exists";

    public CheckerTypeAlreadyExistsException() {
        super(Map.of("type", List.of(MESSAGE_KEY)));
    }

}
