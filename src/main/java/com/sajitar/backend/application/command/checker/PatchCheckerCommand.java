package com.sajitar.backend.application.command.checker;

import java.util.UUID;

import com.sajitar.backend.domain.model.checker.Checker;

import jakarta.validation.constraints.NotNull;

public record PatchCheckerCommand(
        @NotNull(message = "{validation.not-null}") UUID id,
        Checker.Type type,
        String payload) {

    public boolean hasChanges() {
        return type != null || payload != null;
    }

}
