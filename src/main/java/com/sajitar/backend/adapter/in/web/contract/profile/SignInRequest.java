package com.sajitar.backend.adapter.in.web.contract.profile;

import com.sajitar.backend.application.command.profile.SignInProfileCommand;
import com.sajitar.backend.domain.validation.profile.Email;
import com.sajitar.backend.domain.validation.profile.Password;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SignInRequest", description = "Credenciais para emissão de um JWT de acesso.")
public record SignInRequest(
        @Schema(description = "Endereço de e-mail do perfil", example = "alice@example.com")
        @Email String email,
        @Schema(description = "Senha em texto plano", example = "senhaSegura1")
        @Password String password) {

    public SignInProfileCommand toCommand() {
        return new SignInProfileCommand(email, password);
    }

}
