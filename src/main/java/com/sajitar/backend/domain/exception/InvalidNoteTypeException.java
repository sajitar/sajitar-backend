package com.sajitar.backend.domain.exception;

import java.util.List;
import java.util.Map;

public final class InvalidNoteTypeException extends DomainException {

    public static final String MESSAGE_KEY = "validation.note.type.not-found";

    private final String rejectedValue;

    public InvalidNoteTypeException(final String rejectedValue) {
        super(Map.of("type", List.of(MESSAGE_KEY)));
        this.rejectedValue = rejectedValue;
    }

    public String rejectedValue() {
        return rejectedValue;
    }

}
