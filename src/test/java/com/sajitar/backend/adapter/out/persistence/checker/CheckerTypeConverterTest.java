package com.sajitar.backend.adapter.out.persistence.checker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sajitar.backend.domain.exception.InvalidCheckerTypeException;
import com.sajitar.backend.domain.model.checker.Checker;

@DisplayName("CheckerTypeConverter")
class CheckerTypeConverterTest {

    private final CheckerTypeConverter converter = new CheckerTypeConverter();

    @Test
    @DisplayName("Converte enum para smallint e de volta")
    void roundTrip() {
        assertThat(converter.convertToDatabaseColumn(Checker.Type.CHANGE_EMAIL)).isEqualTo((short) 0);
        assertThat(converter.convertToDatabaseColumn(Checker.Type.VERIFY_EMAIL)).isEqualTo((short) 1);
        assertThat(converter.convertToDatabaseColumn(Checker.Type.CHANGE_PASSWORD)).isEqualTo((short) 4);
        assertThat(converter.convertToEntityAttribute((short) 0)).isEqualTo(Checker.Type.CHANGE_EMAIL);
        assertThat(converter.convertToEntityAttribute((short) 1)).isEqualTo(Checker.Type.VERIFY_EMAIL);
        assertThat(converter.convertToEntityAttribute((short) 4)).isEqualTo(Checker.Type.CHANGE_PASSWORD);
    }

    @Test
    @DisplayName("Nulos permanecem nulos")
    void nullsStayNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    @DisplayName("Inteiro desconhecido vira InvalidCheckerTypeException")
    void unknownIntIsRejected() {
        final var thrown = catchThrowable(() -> converter.convertToEntityAttribute((short) 2));
        assertThat(thrown).isInstanceOf(InvalidCheckerTypeException.class);
        assertThat(((InvalidCheckerTypeException) thrown).rejectedValue()).isEqualTo("2");
    }

}
