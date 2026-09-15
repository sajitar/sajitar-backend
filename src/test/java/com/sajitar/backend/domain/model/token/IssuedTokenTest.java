package com.sajitar.backend.domain.model.token;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("IssuedToken")
class IssuedTokenTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    @DisplayName("Expõe o jti das claims e a validade restante")
    void exposesClaimsIdentifier() {
        final var claims = TokenClaims.create(TokenUse.ACCESS, NOW, NOW.plusSeconds(3600));

        final var token = new IssuedToken(claims, "eyJhbGciOiJIUzI1NiJ9.token", 3600L);

        assertThat(token.id()).isEqualTo(claims.id());
        assertThat(token.value()).isEqualTo("eyJhbGciOiJIUzI1NiJ9.token");
        assertThat(token.expiresInSeconds()).isEqualTo(3600L);
        assertThat(token.isExpired()).isFalse();
    }

    @Test
    @DisplayName("Sem validade restante o token já nasce vencido")
    void reportsExpiredWithoutRemainingSeconds() {
        final var claims = TokenClaims.create(TokenUse.REFRESH, NOW, NOW);

        assertThat(new IssuedToken(claims, "eyJ.token", 0L).isExpired()).isTrue();
    }

}
