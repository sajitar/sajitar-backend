package com.sajitar.backend.domain.exception;

import java.util.Map;

public final class NoteNotFoundException extends DomainException {

    public NoteNotFoundException() {
        super(Map.of());
    }

}
