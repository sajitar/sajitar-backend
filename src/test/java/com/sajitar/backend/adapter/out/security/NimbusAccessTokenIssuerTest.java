package com.sajitar.backend.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.sajitar.backend.configuration.JwtProperties;

@DisplayName("NimbusAccessTokenIssuer")
class NimbusAccessTokenIssuerTest {

    @Test
    @DisplayName("Emite par HS256 com token_use distintos e expirações relativas")
    void issuesHs256PairWithDistinctTokenUseAndExpiry() {
        final var secret = "01234567890123456789012345678901";
        final var properties = new JwtProperties(secret, 3600, 604800);
        final var clock = Clock.systemUTC();
        final var key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        final var issuer = new NimbusAccessTokenIssuer(
                new NimbusJwtEncoder(new ImmutableSecret<>(key)),
                properties,
                clock);
        final var profileId = UUID.fromString("01989bad-6161-7000-0ae9-f440b10578ec");
        final var before = Instant.now().minusSeconds(2);

        final var issued = issuer.issue(profileId);

        assertThat(issued.access().expiresInSeconds()).isEqualTo(3600);
        assertThat(issued.refresh().expiresInSeconds()).isEqualTo(604800);
        final var decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        final var accessJwt = decoder.decode(issued.access().value());
        final var refreshJwt = decoder.decode(issued.refresh().value());
        assertThat(accessJwt.getSubject()).isEqualTo(profileId.toString());
        assertThat(refreshJwt.getSubject()).isEqualTo(profileId.toString());
        assertThat(accessJwt.getClaimAsString(JwtTokenUse.CLAIM)).isEqualTo(JwtTokenUse.ACCESS);
        assertThat(refreshJwt.getClaimAsString(JwtTokenUse.CLAIM)).isEqualTo(JwtTokenUse.REFRESH);
        assertThat(accessJwt.getExpiresAt()).isAfter(before.plusSeconds(3598));
        assertThat(accessJwt.getExpiresAt()).isBefore(Instant.now().plusSeconds(3602));
        assertThat(refreshJwt.getExpiresAt()).isAfter(before.plusSeconds(604798));
        assertThat(refreshJwt.getExpiresAt()).isBefore(Instant.now().plusSeconds(604802));
    }

}
