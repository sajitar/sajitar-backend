package com.sajitar.backend.adapter.in.web.contract.token;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sajitar.backend.domain.model.token.IssuedSession;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TokenResponse", description = "Tokens emitidos para uma sessão. Campos de refresh são omitidos quando a sessão tem apenas access.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TokenResponse(
        @Schema(description = "Access token JWT", example = "eyJhbGciOiJIUzI1NiJ9...")
        String token,
        @Schema(description = "Tipo do token", example = "Bearer")
        String type,
        @Schema(description = "Validade do access token em segundos", example = "3600")
        long expiresIn,
        @Schema(description = "Identificador do access token (jti)", example = "018f3c2a-7b00-7c3d-9e1a-000000000001")
        UUID id,
        @Schema(description = "Identificador da sessão de login", example = "018f3c2a-7b00-7c3d-9e1a-000000000010")
        UUID sessionId,
        @Schema(description = "Refresh token JWT", example = "eyJhbGciOiJIUzI1NiJ9...")
        String refreshToken,
        @Schema(description = "Identificador do refresh token (jti)", example = "018f3c2a-7b00-7c3d-9e1a-000000000002")
        UUID refreshId,
        @Schema(description = "Validade do refresh token em segundos", example = "604800")
        Long refreshExpiresIn) {

    private static final String TYPE = "Bearer";

    public static TokenResponse from(final IssuedSession session) {
        final var access = session.access();
        if (!session.hasRefresh()) {
            return new TokenResponse(
                    access.value(),
                    TYPE,
                    access.expiresInSeconds(),
                    access.id(),
                    session.sessionId(),
                    null,
                    null,
                    null);
        }
        final var refresh = session.refresh();
        return new TokenResponse(
                access.value(),
                TYPE,
                access.expiresInSeconds(),
                access.id(),
                session.sessionId(),
                refresh.value(),
                refresh.id(),
                refresh.expiresInSeconds());
    }

}
