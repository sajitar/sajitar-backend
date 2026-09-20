package com.sajitar.backend.domain.port.token;

import java.util.UUID;

import com.sajitar.backend.domain.model.token.Client;
import com.sajitar.backend.domain.model.token.Session;
import com.sajitar.backend.domain.model.token.TokenClaims;

public record RotationCommand(
        UUID presentedRefreshId,
        Session session,
        TokenClaims access,
        TokenClaims refresh,
        Client client) {
}
