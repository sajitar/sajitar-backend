package com.sajitar.backend.domain.model.token;

import java.util.UUID;

public record IssuedToken(TokenClaims claims, String value, long expiresInSeconds) {

    public UUID id() {
        return claims.id();
    }

    public boolean isExpired() {
        return expiresInSeconds <= 0L;
    }

}
