package com.sajitar.backend.domain.exception;

import java.util.List;
import java.util.Map;

public final class EmailAlreadyRegisteredException extends DomainException {

    public EmailAlreadyRegisteredException() {
        super(Map.of("email", List.of("deve ser um e-mail não registrado")));
    }

}
