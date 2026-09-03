package com.sajitar.backend.domain.exception;

import java.util.List;
import java.util.Map;

public final class AuthorityTypeAlreadyExistsException extends DomainException {

    public static final String MESSAGE_KEY = "validation.authority.type.already-exists";

    public AuthorityTypeAlreadyExistsException() {
        super(Map.of("type", List.of(MESSAGE_KEY)));
    }

}
