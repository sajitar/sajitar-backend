package com.sajitar.backend.domain.model.note;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.sajitar.backend.domain.exception.InvalidNoteTypeException;

@DisplayName("Note (agregado)")
class NoteTest {

    private static final UUID PROFILE_ID = UUID.fromString("01989bad-6161-7000-0ae9-f440b10578ec");

    @Test
    @DisplayName("create gera id e preserva profileId, type e content")
    void createGeneratesId() {
        final var created = Note.create(PROFILE_ID, Note.Type.PUBLIC, "Uma nota.");

        assertThat(created.id()).isNotNull();
        assertThat(created.profileId()).isEqualTo(PROFILE_ID);
        assertThat(created.type()).isEqualTo(Note.Type.PUBLIC);
        assertThat(created.content()).isEqualTo("Uma nota.");
    }

    @Test
    @DisplayName("withType copia id, profileId e content")
    void withTypeCopiesRemainingFields() {
        final var original = Note.create(PROFILE_ID, Note.Type.PUBLIC, "Uma nota.");
        final var updated = original.withType(Note.Type.PRIVATE);

        assertThat(updated.id()).isEqualTo(original.id());
        assertThat(updated.profileId()).isEqualTo(original.profileId());
        assertThat(updated.content()).isEqualTo(original.content());
        assertThat(updated.type()).isEqualTo(Note.Type.PRIVATE);
    }

    @Test
    @DisplayName("withContent copia id, profileId e type")
    void withContentCopiesRemainingFields() {
        final var original = Note.create(PROFILE_ID, Note.Type.PUBLIC, "Uma nota.");
        final var updated = original.withContent("Outro texto.");

        assertThat(updated.id()).isEqualTo(original.id());
        assertThat(updated.profileId()).isEqualTo(original.profileId());
        assertThat(updated.type()).isEqualTo(original.type());
        assertThat(updated.content()).isEqualTo("Outro texto.");
    }

    @Test
    @DisplayName("equals considera apenas o id e rejeita outros tipos")
    void equalsByIdOnly() {
        final var id = UUID.randomUUID();
        final var a = new Note(id, PROFILE_ID, Note.Type.PUBLIC, "a");
        final var b = new Note(id, UUID.randomUUID(), Note.Type.PRIVATE, "b");
        final var c = new Note(UUID.randomUUID(), PROFILE_ID, Note.Type.PUBLIC, "a");

        assertThat(a).isEqualTo(b).isNotEqualTo(c).isNotEqualTo("nao-e-note").isNotEqualTo(null);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @ParameterizedTest
    @CsvSource({
            "0, PUBLIC",
            "1, PROTECTED",
            "2, PRIVATE"
    })
    @DisplayName("Type.valueOf(int) e parse pelo nome ou número")
    void typeValueOfIntAndParse(final int value, final Note.Type expected) {
        final var type = Note.Type.valueOf(value);
        assertThat(type).isEqualTo(expected);
        assertThat(type.value()).isEqualTo(value);
        assertThat(Note.Type.parse(expected.name())).isEqualTo(expected);
        assertThat(Note.Type.parse(Integer.toString(value))).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(ints = { 3, 4, -1, 5 })
    @DisplayName("Type.valueOf(int) rejeita valores fora do enum")
    void typeValueOfIntRejectsUnknown(final int value) {
        final var thrown = catchThrowable(() -> Note.Type.valueOf(value));
        assertThat(thrown).isInstanceOf(InvalidNoteTypeException.class);
        assertThat(((InvalidNoteTypeException) thrown).rejectedValue()).isEqualTo(Integer.toString(value));
    }

    @ParameterizedTest
    @ValueSource(strings = { "SECRET", "HIDDEN", "UNKNOWN", "" })
    @DisplayName("Type.parse rejeita nomes desconhecidos")
    void parseRejectsUnknownNames(final String raw) {
        final var thrown = catchThrowable(() -> Note.Type.parse(raw));
        assertThat(thrown).isInstanceOf(InvalidNoteTypeException.class);
        assertThat(((InvalidNoteTypeException) thrown).rejectedValue()).isEqualTo(raw);
    }

    @Test
    @DisplayName("Type.parse rejeita nulo")
    void parseRejectsNull() {
        final var thrown = catchThrowable(() -> Note.Type.parse(null));
        assertThat(thrown).isInstanceOf(InvalidNoteTypeException.class);
        assertThat(((InvalidNoteTypeException) thrown).rejectedValue()).isEqualTo("null");
    }

}
