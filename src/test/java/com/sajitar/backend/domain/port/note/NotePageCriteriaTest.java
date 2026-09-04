package com.sajitar.backend.domain.port.note;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sajitar.backend.domain.model.note.Note;

@DisplayName("NotePageCriteria")
class NotePageCriteriaTest {

    @Test
    @DisplayName("hasCursor é verdadeiro somente quando lastSeenId está preenchido")
    void hasCursorWhenLastSeenIdIsPresent() {
        final var profileId = UUID.randomUUID();
        final var lastSeenId = UUID.randomUUID();
        assertThat(new NotePageCriteria(profileId, null, null, 10, false).hasCursor()).isFalse();
        assertThat(new NotePageCriteria(profileId, Note.Type.PUBLIC, lastSeenId, 10, false).hasCursor()).isTrue();
    }

    @Test
    @DisplayName("hasTypeFilter é verdadeiro somente quando type está preenchido")
    void hasTypeFilterWhenTypeIsPresent() {
        final var profileId = UUID.randomUUID();
        assertThat(new NotePageCriteria(profileId, null, null, 10, false).hasTypeFilter()).isFalse();
        assertThat(new NotePageCriteria(profileId, Note.Type.PROTECTED, null, 10, false).hasTypeFilter()).isTrue();
    }

    @Test
    @DisplayName("withCursor preserva profileId, type e limit e troca id e reverse")
    void withCursorReplacesIdAndReverse() {
        final var profileId = UUID.randomUUID();
        final var originalId = UUID.randomUUID();
        final var nextId = UUID.randomUUID();
        final var original = new NotePageCriteria(profileId, Note.Type.PUBLIC, originalId, 5, false);
        final var next = original.withCursor(nextId, true);
        assertThat(next.profileId()).isEqualTo(profileId);
        assertThat(next.type()).isEqualTo(Note.Type.PUBLIC);
        assertThat(next.limit()).isEqualTo(5);
        assertThat(next.lastSeenId()).isEqualTo(nextId);
        assertThat(next.reverse()).isTrue();
    }

}
