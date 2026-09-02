package com.sajitar.backend.domain.exception;

import java.util.Map;

public final class ProfileNotFoundException extends DomainException {

    public ProfileNotFoundException() {
        super(Map.of());
    }

}
