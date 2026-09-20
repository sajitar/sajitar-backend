package com.sajitar.backend.domain.exception;

import java.util.Map;

public final class SessionNotFoundException extends DomainException {

    public SessionNotFoundException() {
        super(Map.of());
    }

}
