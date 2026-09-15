package com.sajitar.backend.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("JwtProperties")
class JwtPropertiesTest {

    private static final String VALID_SECRET = "01234567890123456789012345678901";

    private static final int EXPIRATION = 3600;

    private static final int REFRESH_EXPIRATION = 604800;

    private static final int SESSION_MAX = 2592000;

    private static final int MAX_SESSIONS = 10;

    private static final int GRACE = 15;

    private static final String ISSUER = "sajitar-backend";

    private static final String AUDIENCE = "sajitar-app";

    @Test
    @DisplayName("Aceita segredo de 32 bytes, janelas crescentes, teto de sessões, graça, issuer e audience")
    void acceptsValidConfiguration() {
        final var properties = properties(VALID_SECRET, EXPIRATION, REFRESH_EXPIRATION, SESSION_MAX, MAX_SESSIONS,
                GRACE, ISSUER, AUDIENCE);

        assertThat(properties.secret()).isEqualTo(VALID_SECRET);
        assertThat(properties.expirationSeconds()).isEqualTo(EXPIRATION);
        assertThat(properties.refreshExpirationSeconds()).isEqualTo(REFRESH_EXPIRATION);
        assertThat(properties.sessionMaxSeconds()).isEqualTo(SESSION_MAX);
        assertThat(properties.maxSessionsPerProfile()).isEqualTo(MAX_SESSIONS);
        assertThat(properties.refreshGraceSeconds()).isEqualTo(GRACE);
        assertThat(properties.issuer()).isEqualTo(ISSUER);
        assertThat(properties.audience()).isEqualTo(AUDIENCE);
    }

    @Test
    @DisplayName("Aceita graça zero, em que qualquer refresh reapresentado derruba a sessão")
    void acceptsZeroGrace() {
        final var properties = properties(VALID_SECRET, EXPIRATION, REFRESH_EXPIRATION, SESSION_MAX, MAX_SESSIONS,
                0, ISSUER, AUDIENCE);

        assertThat(properties.refreshGraceSeconds()).isZero();
    }

    @Test
    @DisplayName("Rejeita segredo nulo")
    void rejectsNullSecret() {
        final var thrown = catchThrowable(() -> properties(null, EXPIRATION, REFRESH_EXPIRATION, SESSION_MAX,
                MAX_SESSIONS, GRACE, ISSUER, AUDIENCE));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("secret");
    }

    @Test
    @DisplayName("Rejeita segredo com menos de 32 bytes")
    void rejectsShortSecret() {
        final var thrown = catchThrowable(() -> properties("too-short-secret-value", EXPIRATION, REFRESH_EXPIRATION,
                SESSION_MAX, MAX_SESSIONS, GRACE, ISSUER, AUDIENCE));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("secret");
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -1 })
    @DisplayName("Rejeita expiração de access não positiva")
    void rejectsNonPositiveExpiration(final int expirationSeconds) {
        final var thrown = catchThrowable(() -> properties(VALID_SECRET, expirationSeconds, REFRESH_EXPIRATION,
                SESSION_MAX, MAX_SESSIONS, GRACE, ISSUER, AUDIENCE));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expiration-seconds");
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -1 })
    @DisplayName("Rejeita expiração de refresh não positiva")
    void rejectsNonPositiveRefreshExpiration(final int refreshExpirationSeconds) {
        final var thrown = catchThrowable(() -> properties(VALID_SECRET, EXPIRATION, refreshExpirationSeconds,
                SESSION_MAX, MAX_SESSIONS, GRACE, ISSUER, AUDIENCE));

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
        final var thrown = catchThrowable(() -> properties(VALID_SECRET, expirationSeconds, refreshExpirationSeconds,
                SESSION_MAX, MAX_SESSIONS, GRACE, ISSUER, AUDIENCE));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("refresh-expiration-seconds")
                .hasMessageContaining("expiration-seconds");
    }

    @ParameterizedTest
    @ValueSource(ints = { 604800, 604799 })
    @DisplayName("Rejeita teto de sessão menor ou igual à janela de inatividade do refresh")
    void rejectsSessionMaxNotGreaterThanRefresh(final int sessionMaxSeconds) {
        final var thrown = catchThrowable(() -> properties(VALID_SECRET, EXPIRATION, REFRESH_EXPIRATION,
                sessionMaxSeconds, MAX_SESSIONS, GRACE, ISSUER, AUDIENCE));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("session-max-seconds");
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -1 })
    @DisplayName("Rejeita teto de sessões por perfil não positivo")
    void rejectsNonPositiveMaxSessions(final int maxSessionsPerProfile) {
        final var thrown = catchThrowable(() -> properties(VALID_SECRET, EXPIRATION, REFRESH_EXPIRATION, SESSION_MAX,
                maxSessionsPerProfile, GRACE, ISSUER, AUDIENCE));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("max-sessions-per-profile");
    }

    @Test
    @DisplayName("Rejeita graça negativa")
    void rejectsNegativeGrace() {
        final var thrown = catchThrowable(() -> properties(VALID_SECRET, EXPIRATION, REFRESH_EXPIRATION, SESSION_MAX,
                MAX_SESSIONS, -1, ISSUER, AUDIENCE));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("refresh-grace-seconds");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   " })
    @DisplayName("Rejeita issuer em branco")
    void rejectsBlankIssuer(final String issuer) {
        final var thrown = catchThrowable(() -> properties(VALID_SECRET, EXPIRATION, REFRESH_EXPIRATION, SESSION_MAX,
                MAX_SESSIONS, GRACE, issuer, AUDIENCE));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("issuer");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   " })
    @DisplayName("Rejeita audience em branco")
    void rejectsBlankAudience(final String audience) {
        final var thrown = catchThrowable(() -> properties(VALID_SECRET, EXPIRATION, REFRESH_EXPIRATION, SESSION_MAX,
                MAX_SESSIONS, GRACE, ISSUER, audience));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("audience");
    }

    private static JwtProperties properties(
            final String secret,
            final int expirationSeconds,
            final int refreshExpirationSeconds,
            final int sessionMaxSeconds,
            final int maxSessionsPerProfile,
            final int refreshGraceSeconds,
            final String issuer,
            final String audience) {
        return new JwtProperties(secret, expirationSeconds, refreshExpirationSeconds, sessionMaxSeconds,
                maxSessionsPerProfile, refreshGraceSeconds, issuer, audience);
    }

}
