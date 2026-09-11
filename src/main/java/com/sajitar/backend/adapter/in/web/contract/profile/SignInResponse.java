package com.sajitar.backend.adapter.in.web.contract.profile;

import com.sajitar.backend.domain.port.TokenPair;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SignInResponse", description = "Par JWT emitido após autenticação ou renovação bem-sucedida.")
public record SignInResponse(
        @Schema(description = "Token JWT de acesso", example = "eyJhbGciOiJIUzI1NiJ9...")
        String token,
        @Schema(description = "Tipo do token", example = "Bearer")
        String type,
        @Schema(description = "Validade do access token em segundos", example = "3600")
        long expiresIn,
        @Schema(description = "Token JWT de refresh", example = "eyJhbGciOiJIUzI1NiJ9...")
        String refreshToken,
        @Schema(description = "Validade do refresh token em segundos", example = "604800")
        long refreshExpiresIn) {

    public static SignInResponse from(final TokenPair pair) {
        return new SignInResponse(
                pair.access().value(),
                "Bearer",
                pair.access().expiresInSeconds(),
                pair.refresh().value(),
                pair.refresh().expiresInSeconds());
    }

}
