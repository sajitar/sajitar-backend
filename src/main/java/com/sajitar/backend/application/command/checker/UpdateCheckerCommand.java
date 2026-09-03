package com.sajitar.backend.application.command.checker;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record UpdateCheckerCommand(
        @NotNull(message = "{validation.not-null}") UUID id,
        String code,
        String payload,
        Integer attempts,
        Integer replaces) {

}
