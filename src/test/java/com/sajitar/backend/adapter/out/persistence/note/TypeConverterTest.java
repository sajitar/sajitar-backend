package com.sajitar.backend.adapter.out.persistence.note;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sajitar.backend.domain.exception.InvalidNoteTypeException;
import com.sajitar.backend.domain.model.note.Note;

@DisplayName("TypeConverter")
class TypeConverterTest {

    private final TypeConverter converter = new TypeConverter();

    @Test
    @DisplayName("Converte enum para smallint e de volta")
    void roundTrip() {
        assertThat(converter.convertToDatabaseColumn(Note.Type.PUBLIC)).isEqualTo((short) 0);
        assertThat(converter.convertToDatabaseColumn(Note.Type.PROTECTED)).isEqualTo((short) 1);
        assertThat(converter.convertToDatabaseColumn(Note.Type.PRIVATE)).isEqualTo((short) 2);
        assertThat(converter.convertToEntityAttribute((short) 0)).isEqualTo(Note.Type.PUBLIC);
        assertThat(converter.convertToEntityAttribute((short) 1)).isEqualTo(Note.Type.PROTECTED);
        assertThat(converter.convertToEntityAttribute((short) 2)).isEqualTo(Note.Type.PRIVATE);
    }

    @Test
    @DisplayName("Nulos permanecem nulos")
    void nullsStayNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    @DisplayName("Inteiro desconhecido vira InvalidNoteTypeException")
    void unknownIntIsRejected() {
        final var thrown = catchThrowable(() -> converter.convertToEntityAttribute((short) 4));
        assertThat(thrown).isInstanceOf(InvalidNoteTypeException.class);
        assertThat(((InvalidNoteTypeException) thrown).rejectedValue()).isEqualTo("4");
    }

}
