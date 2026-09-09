package com.sajitar.backend.application.command.authority;

import java.util.UUID;

import com.sajitar.backend.domain.model.authority.Authority;

import jakarta.validation.constraints.NotNull;

public record UpdateAuthorityCommand(
        @NotNull(message = "{validation.not-null}") UUID id,
        @NotNull(message = "{validation.not-null}") Authority.Type type) {

}
