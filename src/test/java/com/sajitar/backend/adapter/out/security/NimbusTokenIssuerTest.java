package com.sajitar.backend.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import com.sajitar.backend.configuration.JwtPropertiesFixture;
import com.sajitar.backend.domain.model.token.TokenClaims;
import com.sajitar.backend.domain.model.token.TokenUse;

@DisplayName("NimbusTokenIssuer")
class NimbusTokenIssuerTest {

    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.SECONDS);

    private final NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(JwtTestTokens.KEY)
            .macAlgorithm(MacAlgorithm.HS256)
            .build();

    private final NimbusTokenIssuer issuer = new NimbusTokenIssuer(
            JwtTestTokens.encoder(JwtTestTokens.KEY),
            JwtPropertiesFixture.defaults(),
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    @DisplayName("Access carrega jti, iss, aud e token_use, sem id de perfil")
    void issuesAccessWithPinnedClaims() {
        final var access = issuer.issueAccess(NOW);

        final var jwt = decoder.decode(access.value());
        assertThat(jwt.getId()).isEqualTo(access.id().toString());
        assertThat(jwt.getClaimAsString("iss")).isEqualTo(JwtPropertiesFixture.ISSUER);
        assertThat(jwt.getAudience()).containsExactly(JwtPropertiesFixture.AUDIENCE);
        assertThat(jwt.getClaimAsString(JwtClaims.TOKEN_USE)).isEqualTo(TokenUse.ACCESS.value());
        assertThat(jwt.getSubject()).isNull();
        assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plusSeconds(JwtPropertiesFixture.EXPIRATION_SECONDS));
        assertThat(access.expiresInSeconds()).isEqualTo(JwtPropertiesFixture.EXPIRATION_SECONDS);
        assertThat(access.claims().use()).isEqualTo(TokenUse.ACCESS);
    }

    @Test
    @DisplayName("Refresh de sessão recém-aberta expira pela janela de inatividade")
    void issuesRefreshBoundedByIdleWindow() {
        final var refresh = issuer.issueRefresh(NOW, NOW);

        final var jwt = decoder.decode(refresh.value());
        assertThat(jwt.getClaimAsString(JwtClaims.TOKEN_USE)).isEqualTo(TokenUse.REFRESH.value());
        assertThat(refresh.expiresInSeconds()).isEqualTo(JwtPropertiesFixture.REFRESH_EXPIRATION_SECONDS);
        assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plusSeconds(JwtPropertiesFixture.REFRESH_EXPIRATION_SECONDS));
    }

    @Test
    @DisplayName("Refresh de sessão antiga expira no teto absoluto, não na inatividade")
    void issuesRefreshBoundedByAbsoluteCeiling() {
        final var bornAt = NOW.minusSeconds(JwtPropertiesFixture.SESSION_MAX_SECONDS - 60);

        final var refresh = issuer.issueRefresh(NOW, bornAt);

        assertThat(refresh.expiresInSeconds()).isEqualTo(60L);
        assertThat(refresh.claims().expiresAt())
                .isEqualTo(bornAt.plusSeconds(JwtPropertiesFixture.SESSION_MAX_SECONDS));
    }

    @Test
    @DisplayName("Reemissão na graça reassina as mesmas claims com a validade restante")
    void reissuesStoredClaims() {
        final var claims = new TokenClaims(
                java.util.UUID.fromString("018f3c2a-7b00-7c3d-9e1a-000000000002"),
                TokenUse.REFRESH,
                NOW.minusSeconds(10),
                NOW.plusSeconds(100));

        final var reissued = issuer.reissue(claims);

        final var jwt = decoder.decode(reissued.value());
        assertThat(reissued.id()).isEqualTo(claims.id());
        assertThat(reissued.expiresInSeconds()).isEqualTo(100L);
        assertThat(jwt.getId()).isEqualTo(claims.id().toString());
        assertThat(jwt.getIssuedAt()).isEqualTo(claims.issuedAt());
        assertThat(jwt.getExpiresAt()).isEqualTo(claims.expiresAt());
    }

}
