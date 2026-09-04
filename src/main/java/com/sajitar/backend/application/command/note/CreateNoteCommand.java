package com.sajitar.backend.application.command.note;

import java.util.UUID;

import com.sajitar.backend.domain.model.note.Note;
import com.sajitar.backend.domain.validation.note.Content;

import jakarta.validation.constraints.NotNull;

public record CreateNoteCommand(
        @NotNull(message = "{validation.not-null}") UUID profileId,
        @NotNull(message = "{validation.not-null}") Note.Type type,
        @Content String content) {

}
