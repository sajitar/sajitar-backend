package com.sajitar.backend.application.command.checker;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record PatchCheckerCommand(
        @NotNull(message = "{validation.not-null}") UUID id,
        String code,
        String payload,
        Integer attempts,
        Integer replaces) {

    public boolean hasChanges() {
        return code != null || payload != null || attempts != null || replaces != null;
    }

}
