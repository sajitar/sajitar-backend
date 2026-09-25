package com.sajitar.backend.adapter.in.web.contract.profile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sajitar.backend.application.command.profile.ConfirmPasswordRecoveryCommand;
import com.sajitar.backend.domain.validation.checker.Code;
import com.sajitar.backend.domain.validation.profile.Email;
import com.sajitar.backend.domain.validation.profile.Password;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "ConfirmPasswordRecoveryRequest",
        description = "Confirma o código de CHANGE_PASSWORD e grava a senha nova.")
@JsonIgnoreProperties(ignoreUnknown = true)
public record ConfirmPasswordRecoveryRequest(
        @Schema(description = "Endereço de e-mail do perfil", example = "alice@example.com")
        @Email String email,
        @Schema(description = "Código de seis dígitos enviado ao e-mail", example = "123456")
        @Code String code,
        @Schema(description = "Nova senha em texto plano", example = "senhaNovaSegura1")
        @Password String newPassword) {

    public ConfirmPasswordRecoveryCommand toCommand(final String address) {
        return new ConfirmPasswordRecoveryCommand(email, code, newPassword, address);
    }

}
