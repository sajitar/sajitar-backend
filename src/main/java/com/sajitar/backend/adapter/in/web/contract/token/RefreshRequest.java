package com.sajitar.backend.adapter.in.web.contract.token;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sajitar.backend.application.command.token.RefreshTokenCommand;
import com.sajitar.backend.domain.model.token.Client;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "RefreshRequest", description = "Refresh token a ser trocado por um par novo na mesma sessão.")
@JsonIgnoreProperties(ignoreUnknown = true)
public record RefreshRequest(
        @Schema(description = "Refresh JWT recebido no signin ou na rotação anterior", example = "eyJhbGciOiJIUzI1NiJ9...")
        @NotBlank(message = "{validation.not-blank}") String refreshToken,
        @Schema(
                description = "Quando true, o perfil aprova o termo vigente. Obrigatório se termId ainda não for o vigente; ignorado quando já for.",
                example = "true",
                defaultValue = "false")
        Boolean termAccepted) {

    public RefreshRequest {
        if (termAccepted == null) {
            termAccepted = false;
        }
    }

    public RefreshTokenCommand toCommand(final String address, final Client client) {
        return new RefreshTokenCommand(refreshToken, address, client, termAccepted);
    }

}
