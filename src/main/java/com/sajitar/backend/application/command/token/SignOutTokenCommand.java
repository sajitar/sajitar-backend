package com.sajitar.backend.application.command.token;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record SignOutTokenCommand(
        @NotNull UUID profileId,
        @NotNull UUID currentSessionId,
        @NotEmpty(message = "{validation.not-empty}") List<@NotNull(message = "{validation.not-null}") UUID> ids,
        String password) {

    /** Sair de outra sessão exige a senha; repetir a sessão corrente não. */
    public boolean requiresPassword() {
        return ids.stream().anyMatch(id -> !currentSessionId.equals(id));
    }

}
