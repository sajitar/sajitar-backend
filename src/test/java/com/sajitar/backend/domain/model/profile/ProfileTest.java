package com.sajitar.backend.domain.model.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.sajitar.backend.domain.exception.InvalidProfileTypeException;

@DisplayName("Profile (agregado)")
class ProfileTest {

    @Test
    @DisplayName("withEmail e withPassword copiam os demais campos")
    void withersCopyRemainingFields() {
        final var original = Profile.create(
                Profile.Type.READER, "Maria Silva", "desc", LocalDate.parse("1988-01-10"), "a@b.co", "12345678");

        final var byEmail = original.withEmail("c@d.co");
        assertThat(byEmail.id()).isEqualTo(original.id());
        assertThat(byEmail.email()).isEqualTo("c@d.co");
        assertThat(byEmail.name()).isEqualTo(original.name());
        assertThat(byEmail.type()).isEqualTo(Profile.Type.READER);

        final var byPassword = original.withPassword("outraSenha");
        assertThat(byPassword.id()).isEqualTo(original.id());
        assertThat(byPassword.password()).isEqualTo("outraSenha");
        assertThat(byPassword.email()).isEqualTo(original.email());
    }

    @Test
    @DisplayName("withType copia os demais campos")
    void withTypeCopiesRemainingFields() {
        final var original = Profile.create(
                Profile.Type.READER, "Maria Silva", "desc", LocalDate.parse("1988-01-10"), "a@b.co", "12345678");
        final var updated = original.withType(Profile.Type.MASTER);

        assertThat(updated.id()).isEqualTo(original.id());
        assertThat(updated.type()).isEqualTo(Profile.Type.MASTER);
        assertThat(updated.requiresTwoFactor()).isFalse();
        assertThat(updated.twoFactor()).isFalse();
        assertThat(updated.name()).isEqualTo(original.name());
        assertThat(updated.email()).isEqualTo(original.email());
    }

    @Test
    @DisplayName("create de MASTER nasce com twoFactor ligado")
    void createMasterEnablesTwoFactor() {
        final var created = Profile.create(
                Profile.Type.MASTER, "Maria Silva", "desc", LocalDate.parse("1988-01-10"), "a@b.co", "12345678");
        assertThat(created.twoFactor()).isTrue();
        assertThat(created.requiresTwoFactor()).isTrue();
    }

    @Test
    @DisplayName("twoFactor marcado exige segundo fator mesmo em quem não é MASTER")
    void twoFactorRequiresSecondFactor() {
        final var original = Profile.create(
                Profile.Type.WRITER, "Maria Silva", "desc", LocalDate.parse("1988-01-10"), "a@b.co", "12345678");
        assertThat(original.withTwoFactor(true).requiresTwoFactor()).isTrue();
        assertThat(original.withTwoFactor(true).twoFactor()).isTrue();
        assertThat(original.requiresTwoFactor()).isFalse();
    }

    @Test
    @DisplayName("O instante de criação sai dos 48 bits de tempo do id")
    void readsCreationInstantFromIdentifier() {
        final var before = Instant.now().minusSeconds(1);

        final var created = Profile.create(
                Profile.Type.READER, "Maria Silva", "desc", LocalDate.parse("1988-01-10"), "a@b.co", "12345678");

        assertThat(created.twoFactor()).isFalse();
        assertThat(created.requiresTwoFactor()).isFalse();
        assertThat(created.bornAt()).isAfter(before).isBefore(Instant.now().plusSeconds(1));
    }

    @Test
    @DisplayName("equals considera apenas o id e rejeita outros tipos")
    void equalsByIdOnly() {
        final var id = UUID.randomUUID();
        final var a = new Profile(id, Profile.Type.MASTER, "A", null, LocalDate.parse("1988-01-10"), "a@b.co", "12345678", false);
        final var b = new Profile(id, Profile.Type.READER, "B", "x", LocalDate.parse("1990-01-01"), "b@c.co", "87654321", true);
        final var c = a.withId(UUID.randomUUID());

        assertThat(a).isEqualTo(b).isNotEqualTo(c).isNotEqualTo("nao-e-perfil").isNotEqualTo(null);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @ParameterizedTest
    @CsvSource({
            "0, MASTER",
            "1, WRITER",
            "2, READER"
    })
    @DisplayName("Type.valueOf(int) e parse pelo nome ou número")
    void typeValueOfIntAndParse(final int value, final Profile.Type expected) {
        final var type = Profile.Type.valueOf(value);
        assertThat(type).isEqualTo(expected);
        assertThat(type.value()).isEqualTo(value);
        assertThat(Profile.Type.parse(expected.name())).isEqualTo(expected);
        assertThat(Profile.Type.parse(Integer.toString(value))).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "MASTER, MASTER, true",
            "MASTER, WRITER, true",
            "MASTER, READER, true",
            "WRITER, MASTER, false",
            "WRITER, WRITER, true",
            "WRITER, READER, true",
            "READER, MASTER, false",
            "READER, WRITER, false",
            "READER, READER, true"
    })
    @DisplayName("includes aceita o próprio tipo e os de value maior")
    void includesCumulative(final Profile.Type holder, final Profile.Type required, final boolean expected) {
        assertThat(holder.includes(required)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(ints = { 3, 4, -1, 5 })
    @DisplayName("Type.valueOf(int) rejeita valores fora do enum")
    void typeValueOfIntRejectsUnknown(final int value) {
        final var thrown = catchThrowable(() -> Profile.Type.valueOf(value));
        assertThat(thrown).isInstanceOf(InvalidProfileTypeException.class);
        assertThat(((InvalidProfileTypeException) thrown).rejectedValue()).isEqualTo(Integer.toString(value));
    }

    @ParameterizedTest
    @ValueSource(strings = { "ADMIN", "GUEST", "MEMBER", "" })
    @DisplayName("Type.parse rejeita nomes desconhecidos")
    void parseRejectsUnknownNames(final String raw) {
        final var thrown = catchThrowable(() -> Profile.Type.parse(raw));
        assertThat(thrown).isInstanceOf(InvalidProfileTypeException.class);
        assertThat(((InvalidProfileTypeException) thrown).rejectedValue()).isEqualTo(raw);
    }

    @Test
    @DisplayName("Type.parse rejeita nulo")
    void parseRejectsNull() {
        final var thrown = catchThrowable(() -> Profile.Type.parse(null));
        assertThat(thrown).isInstanceOf(InvalidProfileTypeException.class);
        assertThat(((InvalidProfileTypeException) thrown).rejectedValue()).isEqualTo("null");
    }

}
