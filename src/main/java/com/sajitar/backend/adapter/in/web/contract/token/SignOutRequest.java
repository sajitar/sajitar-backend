package com.sajitar.backend.adapter.in.web.contract.token;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sajitar.backend.application.command.token.SignOutTokenCommand;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "SignOutRequest", description = "Sessões a encerrar. A senha é obrigatória quando a lista inclui outra sessão.")
@JsonIgnoreProperties(ignoreUnknown = true)
public record SignOutRequest(
        @Schema(description = "Identificadores das sessões a encerrar", example = "[\"018f3c2a-7b00-7c3d-9e1a-000000000010\"]")
        List<UUID> ids,
        @Schema(description = "Senha em texto plano, exigida para encerrar sessão que não é a corrente", example = "senhaSegura1")
        String password) {

    public SignOutTokenCommand toCommand(final UUID profileId, final UUID currentSessionId, final String address) {
        return new SignOutTokenCommand(profileId, currentSessionId, ids, password, address);
    }

}
