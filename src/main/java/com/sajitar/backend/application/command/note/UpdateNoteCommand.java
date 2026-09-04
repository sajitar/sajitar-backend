package com.sajitar.backend.application.command.note;

import java.util.UUID;

import com.sajitar.backend.domain.model.note.Note;
import com.sajitar.backend.domain.validation.note.Content;

import jakarta.validation.constraints.NotNull;

public record UpdateNoteCommand(
        @NotNull(message = "{validation.not-null}") UUID id,
        @NotNull(message = "{validation.not-null}") Note.Type type,
        @Content String content) {

}
