package com.sajitar.backend.application.command.authority;

import java.util.UUID;

import com.sajitar.backend.domain.model.authority.Authority;

import jakarta.validation.constraints.NotNull;

public record CreateAuthorityCommand(
        @NotNull(message = "{validation.not-null}") UUID profileId,
        @NotNull(message = "{validation.not-null}") Authority.Type type) {

}
