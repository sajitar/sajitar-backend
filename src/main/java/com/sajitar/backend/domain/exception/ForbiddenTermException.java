package com.sajitar.backend.domain.exception;

import java.util.List;
import java.util.Map;

public final class ForbiddenTermException extends DomainException {

    public static final String MESSAGE_KEY = "validation.term.id.forbidden";

    public ForbiddenTermException() {
        super(Map.of("id", List.of(MESSAGE_KEY)));
    }

}
