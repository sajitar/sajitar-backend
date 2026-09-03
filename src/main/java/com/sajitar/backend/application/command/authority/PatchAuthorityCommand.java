package com.sajitar.backend.application.command.authority;

import java.util.UUID;

import com.sajitar.backend.domain.model.authority.Authority;

import jakarta.validation.constraints.NotNull;

public record PatchAuthorityCommand(
        @NotNull(message = "{validation.not-null}") UUID id,
        Authority.Type type) {

    public boolean hasChanges() {
        return type != null;
    }

}
