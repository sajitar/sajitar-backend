package com.sajitar.backend.adapter.in.web.contract.token;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sajitar.backend.application.command.token.SignInTokenCommand;
import com.sajitar.backend.domain.model.token.Client;
import com.sajitar.backend.domain.validation.profile.Email;
import com.sajitar.backend.domain.validation.profile.Password;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SignInRequest", description = "Credenciais para abrir uma sessão e emitir um access token.")
@JsonIgnoreProperties(ignoreUnknown = true)
public record SignInRequest(
        @Schema(description = "Endereço de e-mail do perfil", example = "alice@example.com")
        @Email String email,
        @Schema(description = "Senha em texto plano", example = "senhaSegura1")
        @Password String password,
        @Schema(
                description = "Quando true, a sessão também recebe um refresh token",
                example = "true",
                defaultValue = "false")
        boolean refresh) {

    public SignInTokenCommand toCommand(final String address, final Client client) {
        return new SignInTokenCommand(email, password, refresh, address, client);
    }

}
