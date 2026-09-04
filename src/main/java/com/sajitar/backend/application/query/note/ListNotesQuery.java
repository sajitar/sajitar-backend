package com.sajitar.backend.application.query.note;

import java.util.UUID;

import com.sajitar.backend.domain.model.note.Note;
import com.sajitar.backend.domain.port.note.NotePageCriteria;
import com.sajitar.backend.domain.validation.Limit;

import jakarta.validation.constraints.NotNull;

public record ListNotesQuery(
        @NotNull(message = "{validation.not-null}") UUID profileId,
        Note.Type type,
        UUID lastSeenId,
        @Limit Integer limit,
        @NotNull Boolean reverse) {

    public boolean hasCursor() {
        return lastSeenId != null;
    }

    public NotePageCriteria toCriteria() {
        return new NotePageCriteria(profileId, type, lastSeenId, limit, reverse);
    }

}
