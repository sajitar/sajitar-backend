package com.sajitar.backend.domain.exception;

import java.util.List;
import java.util.Map;

public final class EmailAlreadyRegisteredException extends DomainException {

    public static final String MESSAGE_KEY = "validation.email.already-registered";

    public EmailAlreadyRegisteredException() {
        super(Map.of("email", List.of(MESSAGE_KEY)));
    }

}
