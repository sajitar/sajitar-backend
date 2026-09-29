package com.sajitar.backend.domain.exception;

import java.util.List;
import java.util.Map;

public final class ForbiddenProfileVerifiedException extends DomainException {

    public static final String MESSAGE_KEY = "validation.profile.verified.forbidden";

    public ForbiddenProfileVerifiedException() {
        super(Map.of("verified", List.of(MESSAGE_KEY)));
    }

}
