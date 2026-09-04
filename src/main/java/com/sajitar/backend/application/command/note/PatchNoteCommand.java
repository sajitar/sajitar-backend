package com.sajitar.backend.application.command.note;

import java.util.UUID;

import com.sajitar.backend.application.command.PatchValue;
import com.sajitar.backend.domain.model.note.Note;

import jakarta.validation.constraints.NotNull;

public record PatchNoteCommand(
        @NotNull(message = "{validation.not-null}") UUID id,
        Note.Type type,
        PatchValue<String> content) {

    public PatchNoteCommand {
        content = content == null ? PatchValue.absent() : content;
    }

    public boolean hasChanges() {
        return type != null || content.isPresent();
    }

}
