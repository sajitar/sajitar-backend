package com.sajitar.backend.application.command.checker;

import java.util.UUID;

import com.sajitar.backend.domain.model.checker.Checker;

import jakarta.validation.constraints.NotNull;

public record CreateCheckerCommand(
        @NotNull(message = "{validation.not-null}") UUID profileId,
        @NotNull(message = "{validation.not-null}") Checker.Type type,
        String payload) {

}
