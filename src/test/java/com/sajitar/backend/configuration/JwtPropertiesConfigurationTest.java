package com.sajitar.backend.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@DisplayName("JwtPropertiesConfiguration")
class JwtPropertiesConfigurationTest {

    private final JwtPropertiesConfiguration configuration = new JwtPropertiesConfiguration();

    private final JwtProperties properties = JwtPropertiesFixture.defaults();

    @Test
    @DisplayName("Expõe relógio, chave HMAC-SHA256 e encoder HS256")
    void buildsHs256Codec() {
        final var clock = configuration.clock();
        final var key = configuration.jwtSecretKey(properties);
        final var encoder = configuration.jwtEncoder(key);

        assertThat(clock).isNotNull();
        assertThat(key.getAlgorithm()).isEqualTo("HmacSHA256");
        assertThat(encoder).isInstanceOf(NimbusJwtEncoder.class);
    }

    @Test
    @DisplayName("Encoder assina com a chave da configuração")
    void signsWithConfiguredKey() {
        final var key = configuration.jwtSecretKey(properties);
        final var encoder = configuration.jwtEncoder(key);
        final var now = Instant.now();

        final var jwt = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder()
                        .issuer(properties.issuer())
                        .issuedAt(now)
                        .expiresAt(now.plusSeconds(properties.expirationSeconds()))
                        .build()));

        assertThat(jwt.getTokenValue()).startsWith("eyJ");
        assertThat(jwt.getHeaders()).containsEntry("alg", MacAlgorithm.HS256);
    }

}
