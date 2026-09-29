package com.sajitar.backend.domain.exception;

import java.util.List;
import java.util.Map;

public final class ForbiddenProfileTypeException extends DomainException {

    public static final String MESSAGE_KEY = "validation.profile.type.forbidden";

    public ForbiddenProfileTypeException() {
        super(Map.of("type", List.of(MESSAGE_KEY)));
    }

}
