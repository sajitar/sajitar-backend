package com.sajitar.backend.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public final class ResourceException extends RuntimeException {

    private final HttpStatus status;

    @Builder.Default
    private final Map<String, List<String>> content = new HashMap<>();

}
