package com.sajitar.backend.application.command;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record DeleteCheckerCommand(@NotNull(message = "{validation.not-null}") UUID id) {
}
