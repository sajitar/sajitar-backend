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

import com.sajitar.backend.domain.exception.CheckerReplacesExhaustedException;
import com.sajitar.backend.domain.exception.InvalidCheckerTypeException;

@DisplayName("Checker (agregado)")
class CheckerTest {

    private static final UUID PROFILE_ID = UUID.fromString("01989bad-6161-7000-0ae9-f440b10578ec");

    @Test
    @DisplayName("create gera id, código de 6 dígitos e defaults de criação")
    void createGeneratesIdCodeAndDefaults() {
        final var created = Checker.create(PROFILE_ID, Checker.Type.CHANGE_EMAIL);

        assertThat(created.id()).isNotNull();
        assertThat(created.profileId()).isEqualTo(PROFILE_ID);
        assertThat(created.type()).isEqualTo(Checker.Type.CHANGE_EMAIL);
        assertThat(created.code()).matches("^[0-9]{6}$");
        assertThat(created.payload()).isNull();
        assertThat(created.attempts()).isEqualTo(Checker.ATTEMPTS_MAX);
        assertThat(created.replaces()).isEqualTo(Checker.REPLACES_MAX);
        assertThat(created.updatedAt()).isNotNull();
        assertThat(created.requiredPayload()).isTrue();
    }

    @Test
    @DisplayName("withers copiam os demais campos")
    void withersCopyRemainingFields() {
        final var original = Checker.create(PROFILE_ID, Checker.Type.CHANGE_EMAIL);
        final var later = Instant.parse("2001-04-24T21:00:00Z");

        assertThat(original.withCode("654321").code()).isEqualTo("654321");
        assertThat(original.withCode("654321").profileId()).isEqualTo(original.profileId());
        assertThat(original.withPayload("x").payload()).isEqualTo("x");
        assertThat(original.withAttempts(4).attempts()).isEqualTo(4);
        assertThat(original.withReplaces(1).replaces()).isEqualTo(1);
        assertThat(original.withUpdatedAt(later).updatedAt()).isEqualTo(later);
        assertThat(original.withPayload("x").requiredPayload()).isFalse();
    }

    @Test
    @DisplayName("consumeReplace gera código, restaura attempts e decrementa replaces")
    void consumeReplaceAppliesNewCodeAttemptsAndDecrementsReplaces() {
        final var original = Checker.create(PROFILE_ID, Checker.Type.CHANGE_EMAIL)
                .withAttempts(2)
                .withPayload("old")
                .withUpdatedAt(Instant.parse("2001-04-24T21:00:00Z"));

        final var updated = original.consumeReplace(Checker.Type.CHANGE_PASSWORD, "novo");

        assertThat(updated.id()).isEqualTo(original.id());
        assertThat(updated.profileId()).isEqualTo(original.profileId());
        assertThat(updated.type()).isEqualTo(Checker.Type.CHANGE_PASSWORD);
        assertThat(updated.payload()).isEqualTo("novo");
        assertThat(updated.code()).matches("^[0-9]{6}$");
        assertThat(updated.code()).isNotEqualTo(original.code());
        assertThat(updated.attempts()).isEqualTo(Checker.ATTEMPTS_MAX);
        assertThat(updated.replaces()).isEqualTo(original.replaces() - 1);
        assertThat(updated.updatedAt()).isAfter(original.updatedAt());
    }

    @Test
    @DisplayName("consumeReplace lança quando replaces já é 0")
    void consumeReplaceThrowsWhenReplacesAreZero() {
        final var original = Checker.create(PROFILE_ID, Checker.Type.CHANGE_EMAIL).withReplaces(0);

        final var thrown = catchThrowable(() -> original.consumeReplace(Checker.Type.CHANGE_EMAIL, "novo"));

        assertThat(thrown).isInstanceOf(CheckerReplacesExhaustedException.class);
        assertThat(((CheckerReplacesExhaustedException) thrown).content().get("replaces"))
                .containsExactly(CheckerReplacesExhaustedException.MESSAGE_KEY);
    }

    @Test
    @DisplayName("equals considera apenas o id e rejeita outros tipos")
    void equalsByIdOnly() {
        final var id = UUID.randomUUID();
        final var a = new Checker(id, PROFILE_ID, Checker.Type.CHANGE_EMAIL, "123456", null, 10, 3, Instant.now());
        final var b = new Checker(id, UUID.randomUUID(), Checker.Type.VERIFY_EMAIL, "000000", "p", 0, 0, Instant.now());
        final var c = new Checker(UUID.randomUUID(), PROFILE_ID, Checker.Type.CHANGE_EMAIL, "123456", null, 10, 3, Instant.now());

        assertThat(a).isEqualTo(b).isNotEqualTo(c).isNotEqualTo("nao-e-checker").isNotEqualTo(null);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("requiredPayload segue o tipo")
    void requiredPayloadFollowsType() {
        final var id = UUID.randomUUID();
        final var now = Instant.now();
        final var emailNull = new Checker(id, PROFILE_ID, Checker.Type.CHANGE_EMAIL, "123456", null, 10, 3, now);
        final var emailPresent = emailNull.withPayload("a@b.co");
        final var verify = new Checker(id, PROFILE_ID, Checker.Type.VERIFY_EMAIL, "123456", null, 10, 3, now);
        final var password = new Checker(id, PROFILE_ID, Checker.Type.CHANGE_PASSWORD, "123456", "x", 10, 3, now);

        assertThat(emailNull.requiredPayload()).isTrue();
        assertThat(emailPresent.requiredPayload()).isFalse();
        assertThat(verify.requiredPayload()).isFalse();
        assertThat(password.requiredPayload()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "0, CHANGE_EMAIL, false",
            "1, VERIFY_EMAIL, true",
            "2, CHANGE_PASSWORD, false"
    })
    @DisplayName("Type.valueOf(int) e flags restrict")
    void typeValueOfIntAndRestrict(final int value, final Checker.Type expected, final boolean restrict) {
        final var type = Checker.Type.valueOf(value);
        assertThat(type).isEqualTo(expected);
        assertThat(type.value()).isEqualTo(value);
        assertThat(type.restrict()).isEqualTo(restrict);
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
