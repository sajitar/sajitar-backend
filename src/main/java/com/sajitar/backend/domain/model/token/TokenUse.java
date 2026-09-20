package com.sajitar.backend.domain.model.token;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum TokenUse {

    ACCESS("access"),
    REFRESH("refresh");

    private final String value;

}
