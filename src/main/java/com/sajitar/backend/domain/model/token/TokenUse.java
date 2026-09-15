package com.sajitar.backend.domain.model.token;

public enum TokenUse {

    ACCESS("access"),
    REFRESH("refresh");

    private final String value;

    TokenUse(final String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

}
