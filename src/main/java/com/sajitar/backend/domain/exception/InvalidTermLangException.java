package com.sajitar.backend.domain.exception;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public final class InvalidTermLangException extends DomainException {

    public static final String MESSAGE_KEY = "validation.term.lang.not-found";

    private final String rejectedValue;

    public InvalidTermLangException(final String rejectedValue) {
        super(Map.of("lang", List.of(MESSAGE_KEY)));
        this.rejectedValue = rejectedValue;
    }

}
