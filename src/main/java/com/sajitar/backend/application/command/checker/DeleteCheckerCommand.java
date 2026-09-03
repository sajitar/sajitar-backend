package com.sajitar.backend.application.command.checker;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record DeleteCheckerCommand(@NotNull(message = "{validation.not-null}") UUID id) {
}
