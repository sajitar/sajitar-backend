package com.sajitar.backend.application.command.checker;

import java.util.UUID;

import com.sajitar.backend.domain.model.checker.Checker;

import jakarta.validation.constraints.NotNull;

public record UpdateCheckerCommand(
        @NotNull(message = "{validation.not-null}") UUID id,
        @NotNull(message = "{validation.not-null}") Checker.Type type,
        String payload) {

}
