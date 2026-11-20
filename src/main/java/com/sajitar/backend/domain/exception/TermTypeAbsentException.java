package com.sajitar.backend.domain.exception;

import java.util.List;
import java.util.Map;

public final class TermTypeAbsentException extends DomainException {

    public static final String MESSAGE_KEY = "validation.term.type.absent";

    public TermTypeAbsentException() {
        super(Map.of("type", List.of(MESSAGE_KEY)));
    }

}
