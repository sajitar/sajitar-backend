package com.sajitar.backend.domain.exception;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public final class InvalidAuthorityTypeException extends DomainException {

    public static final String MESSAGE_KEY = "validation.authority.type.not-found";

    private final String rejectedValue;

    public InvalidAuthorityTypeException(final String rejectedValue) {
        super(Map.of("type", List.of(MESSAGE_KEY)));
        this.rejectedValue = rejectedValue;
    }

}
