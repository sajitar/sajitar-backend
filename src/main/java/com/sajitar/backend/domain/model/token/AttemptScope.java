package com.sajitar.backend.domain.model.token;

public enum AttemptScope {

    CREDENTIALS("credentials"),
    REFRESH("refresh");

    private final String value;

    AttemptScope(final String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

}
