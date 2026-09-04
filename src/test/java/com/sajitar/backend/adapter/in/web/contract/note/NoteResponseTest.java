package com.sajitar.backend.adapter.in.web.contract.note;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sajitar.backend.application.page.Page;
import com.sajitar.backend.domain.model.note.Note;

@DisplayName("NoteResponse")
class NoteResponseTest {

    @Test
    @DisplayName("from copia id, profileId, type e content")
    void fromCopiesAllAttributes() {
        final var note = new Note(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                UUID.fromString("01989bad-6161-7000-0ae9-f440b10578ec"),
                Note.Type.PUBLIC,
                "Uma nota.");

        final var response = NoteResponse.from(note);

        assertThat(response.id()).isEqualTo(note.id());
        assertThat(response.profileId()).isEqualTo(note.profileId());
        assertThat(response.type()).isEqualTo(Note.Type.PUBLIC);
        assertThat(response.content()).isEqualTo("Uma nota.");
    }

    @Test
    @DisplayName("página copia content e metadados de Page")
    void pageFromCopiesPage() {
        final var note = Note.create(
                UUID.fromString("01989bad-6161-7000-0ae9-f440b10578ec"),
                Note.Type.PROTECTED,
                "Protegida.");
        final var response = NotePageResponse.from(new Page<>(List.of(note), 1, 2, false));
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().id()).isEqualTo(note.id());
        assertThat(response.content().getFirst().type()).isEqualTo(Note.Type.PROTECTED);
        assertThat(response.content().getFirst().content()).isEqualTo("Protegida.");
        assertThat(response.precedingElements()).isEqualTo(1);
        assertThat(response.followingElements()).isEqualTo(2);
        assertThat(response.reverse()).isFalse();
    }

}
