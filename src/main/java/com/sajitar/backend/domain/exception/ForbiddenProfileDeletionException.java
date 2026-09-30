package com.sajitar.backend.domain.exception;

import java.util.List;
import java.util.Map;

public final class ForbiddenProfileDeletionException extends DomainException {

    public static final String MESSAGE_KEY = "validation.profile.deletion.forbidden";

    public ForbiddenProfileDeletionException() {
        super(Map.of("id", List.of(MESSAGE_KEY)));
    }

}
