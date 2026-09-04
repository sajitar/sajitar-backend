package com.sajitar.backend.domain.port.note;

import java.util.UUID;

import com.sajitar.backend.domain.model.note.Note;

public record NotePageCriteria(UUID profileId, Note.Type type, UUID lastSeenId, int limit, boolean reverse) {

    public boolean hasCursor() {
        return lastSeenId != null;
    }

    public boolean hasTypeFilter() {
        return type != null;
    }

    public NotePageCriteria withCursor(final UUID lastSeenId, final boolean reverse) {
        return new NotePageCriteria(profileId, type, lastSeenId, limit, reverse);
    }

}
