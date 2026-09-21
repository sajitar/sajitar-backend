package com.sajitar.backend.adapter.in.web.contract.token;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sajitar.backend.application.command.token.ResendVerifyEmailCommand;
import com.sajitar.backend.domain.validation.profile.Email;
import com.sajitar.backend.domain.validation.profile.Password;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "VerificationRequest", description = "Credenciais para reenviar o código de VERIFY_EMAIL.")
@JsonIgnoreProperties(ignoreUnknown = true)
public record VerificationRequest(
        @Schema(description = "Endereço de e-mail do perfil", example = "alice@example.com")
        @Email String email,
        @Schema(description = "Senha em texto plano", example = "senhaSegura1")
        @Password String password) {

    public ResendVerifyEmailCommand toCommand(final String address) {
        return new ResendVerifyEmailCommand(email, password, address);
    }

}
