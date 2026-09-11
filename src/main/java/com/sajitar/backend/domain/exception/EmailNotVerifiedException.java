package com.sajitar.backend.domain.exception;

import java.util.List;
import java.util.Map;

public final class EmailNotVerifiedException extends DomainException {

    public static final String MESSAGE_KEY = "validation.email.unverified";

    public EmailNotVerifiedException() {
        super(Map.of("email", List.of(MESSAGE_KEY)));
    }

}
