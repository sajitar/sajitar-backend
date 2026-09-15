package com.sajitar.backend.domain.model.token;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TokenClaims")
class TokenClaimsTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00.750Z");

    @Test
    @DisplayName("Gera jti UUIDv7 e trunca os instantes para segundos inteiros")
    void createsWithTruncatedInstants() {
        final var claims = TokenClaims.create(TokenUse.ACCESS, NOW, NOW.plusSeconds(3600));

        assertThat(claims.id().version()).isEqualTo(7);
        assertThat(claims.use()).isEqualTo(TokenUse.ACCESS);
        assertThat(claims.issuedAt()).isEqualTo(Instant.parse("2026-01-01T10:00:00Z"));
        assertThat(claims.expiresAt()).isEqualTo(Instant.parse("2026-01-01T11:00:00Z"));
    }

    @Test
    @DisplayName("Validade restante é a diferença até o exp")
    void reportsRemainingSeconds() {
        final var claims = TokenClaims.create(TokenUse.REFRESH, NOW, NOW.plusSeconds(600));

        assertThat(claims.expiresInSeconds(NOW)).isEqualTo(600L);
    }

    @Test
    @DisplayName("Validade restante nunca é negativa")
    void neverReportsNegativeRemainingSeconds() {
        final var claims = TokenClaims.create(TokenUse.REFRESH, NOW.minusSeconds(120), NOW.minusSeconds(60));

        assertThat(claims.expiresInSeconds(NOW)).isZero();
    }

}
