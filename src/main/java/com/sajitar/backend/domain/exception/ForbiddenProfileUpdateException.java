package com.sajitar.backend.domain.exception;

import java.util.List;
import java.util.Map;

public final class ForbiddenProfileUpdateException extends DomainException {

    public static final String MESSAGE_KEY = "validation.profile.update.forbidden";

    public ForbiddenProfileUpdateException() {
        super(Map.of("id", List.of(MESSAGE_KEY)));
    }

}
