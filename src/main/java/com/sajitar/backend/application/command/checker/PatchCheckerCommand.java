package com.sajitar.backend.application.command.checker;

import java.util.UUID;

import com.sajitar.backend.application.command.PatchValue;
import com.sajitar.backend.domain.model.checker.Checker;

import jakarta.validation.constraints.NotNull;

public record PatchCheckerCommand(
        @NotNull(message = "{validation.not-null}") UUID id,
        Checker.Type type,
        PatchValue<String> payload) {

    public PatchCheckerCommand {
        payload = payload == null ? PatchValue.absent() : payload;
    }

    public boolean hasChanges() {
        return type != null || payload.isPresent();
    }

}
