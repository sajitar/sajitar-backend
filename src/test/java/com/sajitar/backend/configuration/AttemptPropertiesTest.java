package com.sajitar.backend.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("AttemptProperties")
class AttemptPropertiesTest {

    @Test
    @DisplayName("Aceita tetos e janelas positivos e a decisão sobre X-Forwarded-For")
    void acceptsValidConfiguration() {
        final var properties = AttemptPropertiesFixture.defaults();

        assertThat(properties.credentialsMax()).isEqualTo(AttemptPropertiesFixture.CREDENTIALS_MAX);
        assertThat(properties.credentialsWindowSeconds())
                .isEqualTo(AttemptPropertiesFixture.CREDENTIALS_WINDOW_SECONDS);
        assertThat(properties.refreshMax()).isEqualTo(AttemptPropertiesFixture.REFRESH_MAX);
        assertThat(properties.refreshWindowSeconds()).isEqualTo(AttemptPropertiesFixture.REFRESH_WINDOW_SECONDS);
        assertThat(properties.trustForwardedFor()).isFalse();
        assertThat(AttemptPropertiesFixture.trustingForwardedFor().trustForwardedFor()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -1 })
    @DisplayName("Rejeita teto de credenciais não positivo")
    void rejectsNonPositiveCredentialsMax(final int credentialsMax) {
        final var thrown = catchThrowable(() -> properties(credentialsMax, 300, 30, 60));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("credentials-max");
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -1 })
    @DisplayName("Rejeita janela de credenciais não positiva")
    void rejectsNonPositiveCredentialsWindow(final int credentialsWindowSeconds) {
        final var thrown = catchThrowable(() -> properties(10, credentialsWindowSeconds, 30, 60));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("credentials-window-seconds");
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -1 })
    @DisplayName("Rejeita teto de refresh não positivo")
    void rejectsNonPositiveRefreshMax(final int refreshMax) {
        final var thrown = catchThrowable(() -> properties(10, 300, refreshMax, 60));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("refresh-max");
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -1 })
    @DisplayName("Rejeita janela de refresh não positiva")
    void rejectsNonPositiveRefreshWindow(final int refreshWindowSeconds) {
        final var thrown = catchThrowable(() -> properties(10, 300, 30, refreshWindowSeconds));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("refresh-window-seconds");
    }

    private static AttemptProperties properties(
            final int credentialsMax,
            final int credentialsWindowSeconds,
            final int refreshMax,
            final int refreshWindowSeconds) {
        return new AttemptProperties(
                credentialsMax, credentialsWindowSeconds, refreshMax, refreshWindowSeconds, false);
    }

}
