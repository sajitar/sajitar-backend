package com.sajitar.backend.adapter.out.persistence.authority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sajitar.backend.domain.exception.InvalidAuthorityTypeException;
import com.sajitar.backend.domain.model.authority.Authority;

@DisplayName("AuthorityTypeConverter")
class AuthorityTypeConverterTest {

    private final AuthorityTypeConverter converter = new AuthorityTypeConverter();

    @Test
    @DisplayName("Converte enum para smallint e de volta")
    void roundTrip() {
        assertThat(converter.convertToDatabaseColumn(Authority.Type.MASTER)).isEqualTo((short) 0);
        assertThat(converter.convertToDatabaseColumn(Authority.Type.MEMBER)).isEqualTo((short) 1);
        assertThat(converter.convertToDatabaseColumn(Authority.Type.READER)).isEqualTo((short) 2);
        assertThat(converter.convertToEntityAttribute((short) 0)).isEqualTo(Authority.Type.MASTER);
        assertThat(converter.convertToEntityAttribute((short) 1)).isEqualTo(Authority.Type.MEMBER);
        assertThat(converter.convertToEntityAttribute((short) 2)).isEqualTo(Authority.Type.READER);
    }

    @Test
    @DisplayName("Nulos permanecem nulos")
    void nullsStayNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    @DisplayName("Inteiro desconhecido vira InvalidAuthorityTypeException")
    void unknownIntIsRejected() {
        final var thrown = catchThrowable(() -> converter.convertToEntityAttribute((short) 4));
        assertThat(thrown).isInstanceOf(InvalidAuthorityTypeException.class);
        assertThat(((InvalidAuthorityTypeException) thrown).rejectedValue()).isEqualTo("4");
    }

}
