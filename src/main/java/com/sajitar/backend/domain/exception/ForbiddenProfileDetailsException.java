package com.sajitar.backend.domain.exception;

import java.util.List;
import java.util.Map;

public final class ForbiddenProfileDetailsException extends DomainException {

    public static final String MESSAGE_KEY = "validation.profile.details.forbidden";

    public ForbiddenProfileDetailsException() {
        super(Map.of("id", List.of(MESSAGE_KEY)));
    }

}
