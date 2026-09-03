package com.sajitar.backend.domain.exception;

import java.util.List;
import java.util.Map;

public final class CheckerReplacesExhaustedException extends DomainException {

    public static final String MESSAGE_KEY = "validation.checker.replaces.exhausted";

    public CheckerReplacesExhaustedException() {
        super(Map.of("replaces", List.of(MESSAGE_KEY)));
    }

}
