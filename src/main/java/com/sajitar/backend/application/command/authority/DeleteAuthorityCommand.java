package com.sajitar.backend.application.command.authority;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record DeleteAuthorityCommand(@NotNull(message = "{validation.not-null}") UUID id) {
}
