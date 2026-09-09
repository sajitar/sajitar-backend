package com.sajitar.backend.domain.exception;

import java.util.List;
import java.util.Map;

public final class ProfileUnavailableException extends DomainException {

    public static final String MESSAGE_KEY = "validation.profile.must-be-available";

    public ProfileUnavailableException() {
        super(Map.of("profileId", List.of(MESSAGE_KEY)));
    }

}
