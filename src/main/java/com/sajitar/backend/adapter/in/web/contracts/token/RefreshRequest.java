package com.sajitar.backend.adapter.in.web.contracts.token;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sajitar.backend.application.command.token.RefreshTokenCommand;
import com.sajitar.backend.domain.model.token.Client;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "RefreshRequest", description = "Refresh token a ser trocado por um par novo na mesma sessão.")
@JsonIgnoreProperties(ignoreUnknown = true)
public record RefreshRequest(
        @Schema(description = "Refresh JWT recebido no signin ou na rotação anterior", example = "eyJhbGciOiJIUzI1NiJ9...")
        @NotBlank(message = "{validation.not-blank}") String refreshToken) {

    public RefreshTokenCommand toCommand(final String address, final Client client) {
        return new RefreshTokenCommand(refreshToken, address, client);
    }

}
