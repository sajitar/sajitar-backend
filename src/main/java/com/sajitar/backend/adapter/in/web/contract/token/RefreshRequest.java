package com.sajitar.backend.adapter.in.web.contract.token;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sajitar.backend.application.command.token.RefreshTokenCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "RefreshRequest", description = "Refresh token a ser trocado por um par novo na mesma sessão.")
@JsonIgnoreProperties(ignoreUnknown = true)
public record RefreshRequest(
        @Schema(description = "Refresh JWT recebido no signin ou na rotação anterior", example = "eyJhbGciOiJIUzI1NiJ9...")
        @NotBlank(message = "{validation.not-blank}") String refreshToken) {

    public RefreshTokenCommand toCommand() {
        return new RefreshTokenCommand(refreshToken);
    }

}
