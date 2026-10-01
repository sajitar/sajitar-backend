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
    @DisplayName("Aceita minutos positivos e fuso válido")
    void acceptsValidConfiguration() {
        final var properties = new ProfilePurgeProperties(30, 30, 30, 30, 30, "America/Sao_Paulo");

        assertThat(properties.unverifiedMaxAgeMinutes()).isEqualTo(30);
        assertThat(properties.changePasswordMaxAgeMinutes()).isEqualTo(30);
        assertThat(properties.changeEmailMaxAgeMinutes()).isEqualTo(30);
        assertThat(properties.deleteProfileMaxAgeMinutes()).isEqualTo(30);
        assertThat(properties.signInMaxAgeMinutes()).isEqualTo(30);
        assertThat(properties.unverifiedPurgeZone()).isEqualTo("America/Sao_Paulo");
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -1 })
    @DisplayName("Rejeita idade máxima não positiva")
    void rejectsNonPositiveMaxAgeMinutes(final int minutes) {
        final var thrown = catchThrowable(() -> new ProfilePurgeProperties(minutes, 30, 30, 30, 30, "UTC"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unverified-max-age-minutes");
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -1 })
    @DisplayName("Rejeita prazo de CHANGE_PASSWORD não positivo")
    void rejectsNonPositiveChangePasswordMaxAgeMinutes(final int minutes) {
        final var thrown = catchThrowable(() -> new ProfilePurgeProperties(30, minutes, 30, 30, 30, "UTC"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("change-password-max-age-minutes");
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -1 })
    @DisplayName("Rejeita prazo de CHANGE_EMAIL não positivo")
    void rejectsNonPositiveChangeEmailMaxAgeMinutes(final int minutes) {
        final var thrown = catchThrowable(() -> new ProfilePurgeProperties(30, 30, minutes, 30, 30, "UTC"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("change-email-max-age-minutes");
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -1 })
    @DisplayName("Rejeita prazo de DELETE_PROFILE não positivo")
    void rejectsNonPositiveDeleteProfileMaxAgeMinutes(final int minutes) {
        final var thrown = catchThrowable(() -> new ProfilePurgeProperties(30, 30, 30, minutes, 30, "UTC"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("delete-profile-max-age-minutes");
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -1 })
    @DisplayName("Rejeita prazo de SIGN_IN não positivo")
    void rejectsNonPositiveSignInMaxAgeMinutes(final int minutes) {
        final var thrown = catchThrowable(() -> new ProfilePurgeProperties(30, 30, 30, 30, minutes, "UTC"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sign-in-max-age-minutes");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   " })
    @DisplayName("Rejeita fuso em branco")
    void rejectsBlankZone(final String zone) {
        final var thrown = catchThrowable(() -> new ProfilePurgeProperties(30, 30, 30, 30, 30, zone));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unverified-purge-zone");
    }

    @Test
    @DisplayName("Rejeita fuso desconhecido")
    void rejectsUnknownZone() {
        final var thrown = catchThrowable(() -> new ProfilePurgeProperties(30, 30, 30, 30, 30, "Not/AZone"));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valid time-zone");
    }

}
