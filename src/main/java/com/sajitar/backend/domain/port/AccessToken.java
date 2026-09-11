package com.sajitar.backend.domain.port;

public record AccessToken(String value, long expiresInSeconds) {
}
