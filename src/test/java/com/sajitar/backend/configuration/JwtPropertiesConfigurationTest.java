package com.sajitar.backend.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@DisplayName("JwtPropertiesConfiguration")
class JwtPropertiesConfigurationTest {

    private final JwtPropertiesConfiguration configuration = new JwtPropertiesConfiguration();

    private final JwtProperties properties = new JwtProperties("01234567890123456789012345678901", 3600, 604800);

    @Test
    @DisplayName("Clock, chave HMAC e encoder/decoder HS256")
    void buildsHs256Codec() {
        final var clock = configuration.clock();
        final var key = configuration.jwtSecretKey(properties);
        final var encoder = configuration.jwtEncoder(key);
        final var decoder = configuration.jwtDecoder(key);

        assertThat(clock).isNotNull();
        assertThat(key.getAlgorithm()).isEqualTo("HmacSHA256");
        assertThat(encoder).isInstanceOf(NimbusJwtEncoder.class);
        assertThat(decoder).isInstanceOf(NimbusJwtDecoder.class);
    }

    @Test
    @DisplayName("Decoder da API aceita token_use access e rejeita refresh")
    void accessDecoderRejectsRefreshTokenUse() {
        final var key = configuration.jwtSecretKey(properties);
        final var encoder = configuration.jwtEncoder(key);
        final var decoder = configuration.jwtDecoder(key);
        final var now = Instant.now();
        final var profileId = "01989bad-6161-7000-0ae9-f440b10578ec";
        final var header = JwsHeader.with(MacAlgorithm.HS256).build();
        final var access = encoder.encode(JwtEncoderParameters.from(header, JwtClaimsSet.builder()
                .subject(profileId)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim(JwtPropertiesConfiguration.TOKEN_USE_CLAIM, JwtPropertiesConfiguration.TOKEN_USE_ACCESS)
                .build()));
        final var refresh = encoder.encode(JwtEncoderParameters.from(header, JwtClaimsSet.builder()
                .subject(profileId)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(604800))
                .claim(JwtPropertiesConfiguration.TOKEN_USE_CLAIM, "refresh")
                .build()));

        assertThat(decoder.decode(access.getTokenValue()).getSubject()).isEqualTo(profileId);
        assertThatThrownBy(() -> decoder.decode(refresh.getTokenValue())).isInstanceOf(JwtException.class);
    }

}
