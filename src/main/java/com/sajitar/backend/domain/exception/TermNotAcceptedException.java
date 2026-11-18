package com.sajitar.backend.domain.exception;

import java.util.List;
import java.util.Map;

public final class TermNotAcceptedException extends DomainException {

    public static final String MESSAGE_KEY = "validation.term.required";

    public TermNotAcceptedException() {
        super(Map.of("termAccepted", List.of(MESSAGE_KEY)));
    }

}
