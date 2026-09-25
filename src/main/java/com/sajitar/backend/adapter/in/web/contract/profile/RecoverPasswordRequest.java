package com.sajitar.backend.adapter.in.web.contract.profile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sajitar.backend.application.command.profile.RequestPasswordRecoveryCommand;
import com.sajitar.backend.domain.validation.profile.Email;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "RecoverPasswordRequest",
        description = "Pede ou reenvia o código de CHANGE_PASSWORD. Sempre 204, sem revelar se o e-mail existe.")
@JsonIgnoreProperties(ignoreUnknown = true)
public record RecoverPasswordRequest(
        @Schema(description = "Endereço de e-mail do perfil", example = "alice@example.com")
        @Email String email) {

    public RequestPasswordRecoveryCommand toCommand(final String address) {
        return new RequestPasswordRecoveryCommand(email, address);
    }

}
