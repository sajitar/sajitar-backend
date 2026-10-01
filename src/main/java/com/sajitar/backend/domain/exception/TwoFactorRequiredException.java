package com.sajitar.backend.domain.exception;

import java.util.List;
import java.util.Map;

public final class TwoFactorRequiredException extends DomainException {

    public static final String MESSAGE_KEY = "validation.email.two-factor";

    public TwoFactorRequiredException() {
        super(Map.of("email", List.of(MESSAGE_KEY)));
    }

}
