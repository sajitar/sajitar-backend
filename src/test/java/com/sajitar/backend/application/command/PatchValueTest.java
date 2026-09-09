package com.sajitar.backend.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PatchValue")
class PatchValueTest {

    @Test
    @DisplayName("absent não está presente e devolve o fallback em orElse")
    void absentIsNotPresentAndUsesFallback() {
        final PatchValue<String> absent = PatchValue.absent();

        assertThat(absent.isPresent()).isFalse();
        assertThat(absent.orElse("fallback")).isEqualTo("fallback");
        assertThat(absent).isEqualTo(PatchValue.absent());
        assertThat(absent.toString()).isEqualTo("PatchValue.absent");
    }

    @Test
    @DisplayName("of guarda o valor, inclusive null")
    void ofKeepsValueIncludingNull() {
        final PatchValue<String> present = PatchValue.of("Maria Silva");
        final PatchValue<String> presentNull = PatchValue.of(null);

        assertThat(present.isPresent()).isTrue();
        assertThat(present.orElse("fallback")).isEqualTo("Maria Silva");
        assertThat(present.toString()).isEqualTo("PatchValue[Maria Silva]");

        assertThat(presentNull.isPresent()).isTrue();
        assertThat(presentNull.orElse("fallback")).isNull();
        assertThat(presentNull.toString()).isEqualTo("PatchValue[null]");
    }

    @Test
    @DisplayName("equals e hashCode distinguem ausente de presente com null")
    void equalsAndHashCodeDistinguishAbsentFromPresentNull() {
        final var absent = PatchValue.absent();
        final var presentNull = PatchValue.of(null);
        final var presentA = PatchValue.of("a");
        final var presentAAgain = PatchValue.of("a");
        final var presentB = PatchValue.of("b");

        assertThat(absent).isEqualTo(PatchValue.absent());
        assertThat(absent).hasSameHashCodeAs(PatchValue.absent());
        assertThat(absent).isNotEqualTo(presentNull);
        assertThat(presentNull).isEqualTo(PatchValue.of(null));
        assertThat(presentNull).hasSameHashCodeAs(PatchValue.of(null));
        assertThat(presentA).isEqualTo(presentAAgain);
        assertThat(presentA).hasSameHashCodeAs(presentAAgain);
        assertThat(presentA).isNotEqualTo(presentB);
        assertThat(presentA).isNotEqualTo("a");
        assertThat(presentA).isNotEqualTo(null);
    }

}
