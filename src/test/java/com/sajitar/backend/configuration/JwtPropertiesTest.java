package com.sajitar.backend.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("JwtProperties")
class JwtPropertiesTest {

    private static final String VALID_SECRET = "01234567890123456789012345678901";

    @Test
    @DisplayName("Aceita segredo de 32 bytes, access positivo e refresh maior que o access")
    void acceptsValidSecretAndExpirations() {
        final var properties = new JwtProperties(VALID_SECRET, 3600, 604800);

        assertThat(properties.secret()).isEqualTo(VALID_SECRET);
        assertThat(properties.expirationSeconds()).isEqualTo(3600);
        assertThat(properties.refreshExpirationSeconds()).isEqualTo(604800);
    }

    @Test
    @DisplayName("Rejeita segredo nulo")
    void rejectsNullSecret() {
        final var thrown = catchThrowable(() -> new JwtProperties(null, 3600, 604800));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("secret");
    }

    @Test
    @DisplayName("Rejeita segredo com menos de 32 bytes")
    void rejectsShortSecret() {
        final var thrown = catchThrowable(() -> new JwtProperties("too-short-secret-value", 3600, 604800));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("secret");
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -1 })
    @DisplayName("Rejeita expiração de access não positiva")
    void rejectsNonPositiveExpiration(final int expirationSeconds) {
        final var thrown = catchThrowable(() -> new JwtProperties(VALID_SECRET, expirationSeconds, 604800));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expiration-seconds");
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -1 })
    @DisplayName("Rejeita expiração de refresh não positiva")
    void rejectsNonPositiveRefreshExpiration(final int refreshExpirationSeconds) {
        final var thrown = catchThrowable(() -> new JwtProperties(VALID_SECRET, 3600, refreshExpirationSeconds));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("refresh-expiration-seconds");
    }

    @ParameterizedTest
    @CsvSource({
            "3600, 3600",
            "3600, 1"
    })
    @DisplayName("Rejeita refresh menor ou igual ao access")
    void rejectsRefreshNotGreaterThanAccess(final int expirationSeconds, final int refreshExpirationSeconds) {
        final var thrown = catchThrowable(
                () -> new JwtProperties(VALID_SECRET, expirationSeconds, refreshExpirationSeconds));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("refresh-expiration-seconds")
                .hasMessageContaining("expiration-seconds");
    }

}
