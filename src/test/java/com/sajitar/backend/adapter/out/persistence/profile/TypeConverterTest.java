package com.sajitar.backend.adapter.out.persistence.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sajitar.backend.domain.exception.InvalidProfileTypeException;
import com.sajitar.backend.domain.model.profile.Profile;

@DisplayName("TypeConverter (profile)")
class TypeConverterTest {

    private final TypeConverter converter = new TypeConverter();

    @Test
    @DisplayName("Converte os três tipos e nulo nos dois sentidos")
    void convertsTypesAndNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        assertThat(converter.convertToDatabaseColumn(Profile.Type.MASTER)).isEqualTo((short) 0);
        assertThat(converter.convertToDatabaseColumn(Profile.Type.WRITER)).isEqualTo((short) 1);
        assertThat(converter.convertToDatabaseColumn(Profile.Type.READER)).isEqualTo((short) 2);
        assertThat(converter.convertToEntityAttribute((short) 0)).isEqualTo(Profile.Type.MASTER);
        assertThat(converter.convertToEntityAttribute((short) 1)).isEqualTo(Profile.Type.WRITER);
        assertThat(converter.convertToEntityAttribute((short) 2)).isEqualTo(Profile.Type.READER);
    }

    @Test
    @DisplayName("Inteiro desconhecido vira InvalidProfileTypeException")
    void unknownIntIsRejected() {
        final var thrown = catchThrowable(() -> converter.convertToEntityAttribute((short) 4));
        assertThat(thrown).isInstanceOf(InvalidProfileTypeException.class);
        assertThat(((InvalidProfileTypeException) thrown).rejectedValue()).isEqualTo("4");
    }

}
