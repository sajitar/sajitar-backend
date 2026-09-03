package com.sajitar.backend.domain.exception;

import java.util.List;
import java.util.Map;

public final class CheckerTypeRestrictedException extends DomainException {

    public static final String CREATE_KEY = "validation.checker.type.create-restricted";

    public static final String DELETE_KEY = "validation.checker.type.delete-restricted";

    private CheckerTypeRestrictedException(final String key) {
        super(Map.of("type", List.of(key)));
    }

    public static CheckerTypeRestrictedException forCreate() {
        return new CheckerTypeRestrictedException(CREATE_KEY);
    }

    public static CheckerTypeRestrictedException forDelete() {
        return new CheckerTypeRestrictedException(DELETE_KEY);
    }

}
