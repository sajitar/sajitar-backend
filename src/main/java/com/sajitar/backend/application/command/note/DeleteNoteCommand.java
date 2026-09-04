package com.sajitar.backend.application.command.note;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record DeleteNoteCommand(@NotNull(message = "{validation.not-null}") UUID id) {
}
