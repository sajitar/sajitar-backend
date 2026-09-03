package com.sajitar.backend.domain.model.authority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.sajitar.backend.domain.exception.InvalidAuthorityTypeException;

@DisplayName("Authority (agregado)")
class AuthorityTest {

    private static final UUID PROFILE_ID = UUID.fromString("01989bad-6161-7000-0ae9-f440b10578ec");

    @Test
    @DisplayName("create gera id e preserva profileId e type")
    void createGeneratesId() {
        final var created = Authority.create(PROFILE_ID, Authority.Type.MASTER);

        assertThat(created.id()).isNotNull();
        assertThat(created.profileId()).isEqualTo(PROFILE_ID);
        assertThat(created.type()).isEqualTo(Authority.Type.MASTER);
    }

    @Test
    @DisplayName("withType copia id e profileId")
    void withTypeCopiesRemainingFields() {
        final var original = Authority.create(PROFILE_ID, Authority.Type.MASTER);
        final var updated = original.withType(Authority.Type.READER);

        assertThat(updated.id()).isEqualTo(original.id());
        assertThat(updated.profileId()).isEqualTo(original.profileId());
        assertThat(updated.type()).isEqualTo(Authority.Type.READER);
    }

    @Test
    @DisplayName("equals considera apenas o id e rejeita outros tipos")
    void equalsByIdOnly() {
        final var id = UUID.randomUUID();
        final var a = new Authority(id, PROFILE_ID, Authority.Type.MASTER);
        final var b = new Authority(id, UUID.randomUUID(), Authority.Type.READER);
        final var c = new Authority(UUID.randomUUID(), PROFILE_ID, Authority.Type.MASTER);

        assertThat(a).isEqualTo(b).isNotEqualTo(c).isNotEqualTo("nao-e-authority").isNotEqualTo(null);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @ParameterizedTest
    @CsvSource({
            "0, MASTER",
            "1, MEMBER",
            "2, READER"
    })
    @DisplayName("Type.valueOf(int) e parse pelo nome ou número")
    void typeValueOfIntAndParse(final int value, final Authority.Type expected) {
        final var type = Authority.Type.valueOf(value);
        assertThat(type).isEqualTo(expected);
        assertThat(type.value()).isEqualTo(value);
        assertThat(Authority.Type.parse(expected.name())).isEqualTo(expected);
        assertThat(Authority.Type.parse(Integer.toString(value))).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(ints = { 3, 4, -1, 5 })
    @DisplayName("Type.valueOf(int) rejeita valores fora do enum")
    void typeValueOfIntRejectsUnknown(final int value) {
        final var thrown = catchThrowable(() -> Authority.Type.valueOf(value));
        assertThat(thrown).isInstanceOf(InvalidAuthorityTypeException.class);
        assertThat(((InvalidAuthorityTypeException) thrown).rejectedValue()).isEqualTo(Integer.toString(value));
    }

    @ParameterizedTest
    @ValueSource(strings = { "ADMIN", "GUEST", "UNKNOWN", "" })
    @DisplayName("Type.parse rejeita nomes desconhecidos")
    void parseRejectsUnknownNames(final String raw) {
        final var thrown = catchThrowable(() -> Authority.Type.parse(raw));
        assertThat(thrown).isInstanceOf(InvalidAuthorityTypeException.class);
        assertThat(((InvalidAuthorityTypeException) thrown).rejectedValue()).isEqualTo(raw);
    }

    @Test
    @DisplayName("Type.parse rejeita nulo")
    void parseRejectsNull() {
        final var thrown = catchThrowable(() -> Authority.Type.parse(null));
        assertThat(thrown).isInstanceOf(InvalidAuthorityTypeException.class);
        assertThat(((InvalidAuthorityTypeException) thrown).rejectedValue()).isEqualTo("null");
    }

}
