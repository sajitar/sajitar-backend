package com.sajitar.backend.domain.exception;

import java.util.Map;

public final class AuthorityNotFoundException extends DomainException {

    public AuthorityNotFoundException() {
        super(Map.of());
    }

}
