package com.sajitar.backend.domain.exception;

import java.util.List;
import java.util.Map;

public abstract sealed class DomainException extends RuntimeException
        permits EmailAlreadyRegisteredException, ProfileNotFoundException, CheckerNotFoundException,
        ProfileUnavailableException, CheckerTypeAlreadyExistsException, CheckerTypeRestrictedException,
        InvalidCheckerTypeException, CheckerReplacesExhaustedException {

    private final Map<String, List<String>> content;

    protected DomainException(final Map<String, List<String>> content) {
        this.content = Map.copyOf(content);
    }

    public Map<String, List<String>> content() {
        return content;
    }

}
