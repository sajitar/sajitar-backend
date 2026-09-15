package com.sajitar.backend.domain.model.token;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("IssuedSession")
class IssuedSessionTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private static final UUID SESSION_ID = UUID.fromString("018f3c2a-7b00-7c3d-9e1a-000000000010");

    @Test
    @DisplayName("Sessão com par carrega access e refresh")
    void carriesBothTokens() {
        final var access = token(TokenUse.ACCESS, 3600L);
        final var refresh = token(TokenUse.REFRESH, 604800L);

        final var session = new IssuedSession(SESSION_ID, access, refresh);

        assertThat(session.sessionId()).isEqualTo(SESSION_ID);
        assertThat(session.access()).isEqualTo(access);
        assertThat(session.refresh()).isEqualTo(refresh);
        assertThat(session.hasRefresh()).isTrue();
    }

    @Test
    @DisplayName("Signin sem refresh devolve sessão só com access")
    void carriesAccessOnly() {
        final var session = new IssuedSession(SESSION_ID, token(TokenUse.ACCESS, 3600L), null);

        assertThat(session.hasRefresh()).isFalse();
        assertThat(session.refresh()).isNull();
    }

    private static IssuedToken token(final TokenUse use, final long expiresInSeconds) {
        return new IssuedToken(
                TokenClaims.create(use, NOW, NOW.plusSeconds(expiresInSeconds)),
                "eyJhbGciOiJIUzI1NiJ9." + use.value(),
                expiresInSeconds);
    }

}
