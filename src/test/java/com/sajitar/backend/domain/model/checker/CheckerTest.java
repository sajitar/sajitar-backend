package com.sajitar.backend.domain.model.checker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.sajitar.backend.domain.exception.InvalidCheckerTypeException;

@DisplayName("Checker (agregado)")
class CheckerTest {

    private static final UUID PROFILE_ID = UUID.fromString("01989bad-6161-7000-0ae9-f440b10578ec");

    @Test
    @DisplayName("create gera id e código de 6 dígitos")
    void createGeneratesIdAndCode() {
        final var created = Checker.create(PROFILE_ID, Checker.Type.CHANGE_EMAIL);

        assertThat(created.id()).isNotNull();
        assertThat(created.profileId()).isEqualTo(PROFILE_ID);
        assertThat(created.type()).isEqualTo(Checker.Type.CHANGE_EMAIL);
        assertThat(created.code()).matches("^[0-9]{6}$");
        assertThat(created.payload()).isNull();
        assertThat(created.requiredPayload()).isTrue();
    }

    @Test
    @DisplayName("uuidV7At coloca o instante nos 48 bits e marca versão 7")
    void uuidV7AtEncodesTimestampAndVersion() {
        final var instant = Instant.parse("2026-09-19T03:00:00Z");
        final var id = Checker.uuidV7At(instant);

        assertThat(id.version()).isEqualTo(7);
        assertThat(id.variant()).isEqualTo(2);
        assertThat(id.getMostSignificantBits() >>> 16).isEqualTo(instant.toEpochMilli());
        assertThat(id.getLeastSignificantBits()).isEqualTo(0x8000000000000000L);
    }

    @Test
    @DisplayName("createdBefore compara o id com o UUIDv7 do corte")
    void createdBeforeComparesIdWithCutoff() {
        final var cutoff = Instant.parse("2026-09-19T15:00:00Z");
        final var older = new Checker(
                Checker.uuidV7At(cutoff.minusSeconds(1)),
                PROFILE_ID,
                Checker.Type.CHANGE_PASSWORD,
                "123456",
                null);
        final var atCutoff = new Checker(
                Checker.uuidV7At(cutoff),
                PROFILE_ID,
                Checker.Type.CHANGE_PASSWORD,
                "123456",
                null);

        assertThat(older.createdBefore(cutoff)).isTrue();
        assertThat(atCutoff.createdBefore(cutoff)).isFalse();
    }

    @Test
    @DisplayName("withers copiam os demais campos")
    void withersCopyRemainingFields() {
        final var original = Checker.create(PROFILE_ID, Checker.Type.CHANGE_EMAIL);

        assertThat(original.withCode("654321").code()).isEqualTo("654321");
        assertThat(original.withCode("654321").profileId()).isEqualTo(original.profileId());
        assertThat(original.withPayload("x").payload()).isEqualTo("x");
        assertThat(original.withPayload("x").requiredPayload()).isFalse();
    }

    @Test
    @DisplayName("rotate gera código novo e aplica type/payload mantendo id e profileId")
    void rotateAppliesNewCodeTypeAndPayload() {
        final var original = Checker.create(PROFILE_ID, Checker.Type.CHANGE_EMAIL).withPayload("old");

        final var updated = original.rotate(Checker.Type.CHANGE_PASSWORD, "novo");

        assertThat(updated.id()).isEqualTo(original.id());
        assertThat(updated.profileId()).isEqualTo(original.profileId());
        assertThat(updated.type()).isEqualTo(Checker.Type.CHANGE_PASSWORD);
        assertThat(updated.payload()).isEqualTo("novo");
        assertThat(updated.code()).matches("^[0-9]{6}$");
        assertThat(updated.code()).isNotEqualTo(original.code());
    }

    @Test
    @DisplayName("equals considera apenas o id e rejeita outros tipos")
    void equalsByIdOnly() {
        final var id = UUID.randomUUID();
        final var a = new Checker(id, PROFILE_ID, Checker.Type.CHANGE_EMAIL, "123456", null);
        final var b = new Checker(id, UUID.randomUUID(), Checker.Type.VERIFY_EMAIL, "000000", "p");
        final var c = new Checker(UUID.randomUUID(), PROFILE_ID, Checker.Type.CHANGE_EMAIL, "123456", null);

        assertThat(a).isEqualTo(b).isNotEqualTo(c).isNotEqualTo("nao-e-checker").isNotEqualTo(null);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("requiredPayload segue o tipo")
    void requiredPayloadFollowsType() {
        final var id = UUID.randomUUID();
        final var emailNull = new Checker(id, PROFILE_ID, Checker.Type.CHANGE_EMAIL, "123456", null);
        final var emailPresent = emailNull.withPayload("a@b.co");
        final var verify = new Checker(id, PROFILE_ID, Checker.Type.VERIFY_EMAIL, "123456", null);
        final var password = new Checker(id, PROFILE_ID, Checker.Type.CHANGE_PASSWORD, "123456", "x");

        assertThat(emailNull.requiredPayload()).isTrue();
        assertThat(emailPresent.requiredPayload()).isFalse();
        assertThat(verify.requiredPayload()).isFalse();
        assertThat(password.requiredPayload()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "0, CHANGE_EMAIL",
            "1, VERIFY_EMAIL",
            "2, CHANGE_PASSWORD"
    })
    @DisplayName("Type.valueOf(int) e parse")
    void typeValueOfIntAndParse(final int value, final Checker.Type expected) {
        final var type = Checker.Type.valueOf(value);
        assertThat(type).isEqualTo(expected);
        assertThat(type.value()).isEqualTo(value);
        assertThat(Checker.Type.parse(expected.name())).isEqualTo(expected);
        assertThat(Checker.Type.parse(Integer.toString(value))).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(ints = { 3, 4, -1, 5 })
    @DisplayName("Type.valueOf(int) rejeita valores fora do enum")
    void typeValueOfIntRejectsUnknown(final int value) {
        final var thrown = catchThrowable(() -> Checker.Type.valueOf(value));
        assertThat(thrown).isInstanceOf(InvalidCheckerTypeException.class);
        assertThat(((InvalidCheckerTypeException) thrown).rejectedValue()).isEqualTo(Integer.toString(value));
    }

    @ParameterizedTest
    @ValueSource(strings = { "CHANGE_PHONE", "VERIFY_PHONE", "UNKNOWN", "" })
    @DisplayName("Type.parse rejeita nomes desconhecidos")
    void parseRejectsUnknownNames(final String raw) {
        final var thrown = catchThrowable(() -> Checker.Type.parse(raw));
        assertThat(thrown).isInstanceOf(InvalidCheckerTypeException.class);
        assertThat(((InvalidCheckerTypeException) thrown).rejectedValue()).isEqualTo(raw);
    }

    @Test
    @DisplayName("Type.parse rejeita nulo")
    void parseRejectsNull() {
        final var thrown = catchThrowable(() -> Checker.Type.parse(null));
        assertThat(thrown).isInstanceOf(InvalidCheckerTypeException.class);
        assertThat(((InvalidCheckerTypeException) thrown).rejectedValue()).isEqualTo("null");
    }

}
