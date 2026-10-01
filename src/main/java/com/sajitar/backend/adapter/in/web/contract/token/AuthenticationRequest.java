package com.sajitar.backend.adapter.in.web.contract.token;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sajitar.backend.application.command.token.RequestSignInCodeCommand;
import com.sajitar.backend.domain.validation.profile.Email;
import com.sajitar.backend.domain.validation.profile.Password;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AuthenticationRequest", description = "Credenciais para pedir o código de SIGN_IN.")
@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthenticationRequest(
        @Schema(description = "Endereço de e-mail do perfil", example = "alice@example.com")
        @Email String email,
        @Schema(description = "Senha em texto plano", example = "senhaSegura1")
        @Password String password) {

    public RequestSignInCodeCommand toCommand(final String address) {
        return new RequestSignInCodeCommand(email, password, address);
    }

}
