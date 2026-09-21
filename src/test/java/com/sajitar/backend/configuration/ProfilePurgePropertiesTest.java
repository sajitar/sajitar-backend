package com.sajitar.backend.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("ProfilePurgeProperties")
class ProfilePurgePropertiesTest {

    @Test
    @DisplayName("Aceita horas positivas e fuso válido")
    void acceptsValidConfiguration() {
        final var properties = new ProfilePurgeProperties(48, "America/Sao_Paulo");

        assertThat(properties.unverifiedMaxAgeHours()).isEqualTo(48);
        assertThat(properties.unverifiedPurgeZone()).isEqualTo("America/Sao_Paulo");
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -1 })
    @DisplayName("Rejeita idade máxima não positiva")
    void rejectsNonPositiveMaxAgeHours(final int hours) {
        final var thrown = catchThrowable(() -> new ProfilePurgeProperties(hours, "UTC"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unverified-max-age-hours");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   " })
    @DisplayName("Rejeita fuso em branco")
    void rejectsBlankZone(final String zone) {
        final var thrown = catchThrowable(() -> new ProfilePurgeProperties(48, zone));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unverified-purge-zone");
    }

    @Test
    @DisplayName("Rejeita fuso desconhecido")
    void rejectsUnknownZone() {
        final var thrown = catchThrowable(() -> new ProfilePurgeProperties(48, "Not/AZone"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valid time-zone");
    }

}
