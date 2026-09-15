package com.sajitar.backend.domain.exception;

import java.util.Map;

public final class SessionStoreUnavailableException extends DomainException {

    public SessionStoreUnavailableException() {
        super(Map.of());
    }

}
