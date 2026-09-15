package com.sajitar.backend.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sajitar.backend.configuration.JwtPropertiesFixture;
import com.sajitar.backend.domain.exception.InvalidRefreshTokenException;
import com.sajitar.backend.domain.model.token.TokenUse;

@DisplayName("NimbusRefreshTokenDecoder")
class NimbusRefreshTokenDecoderTest {

    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.SECONDS);

    private static final String JTI = "018f3c2a-7b00-7c3d-9e1a-000000000002";

    private final NimbusRefreshTokenDecoder decoder = new NimbusRefreshTokenDecoder(
            JwtTestTokens.KEY,
            JwtPropertiesFixture.defaults());

    @Test
    @DisplayName("Devolve o jti de um refresh válido")
    void acceptsValidRefreshToken() {
        final var token = JwtTestTokens.token(JwtTestTokens.KEY, JTI, TokenUse.REFRESH.value(),
                JwtPropertiesFixture.ISSUER, List.of(JwtPropertiesFixture.AUDIENCE), NOW, NOW.plusSeconds(604800));

        assertThat(decoder.refreshId(token)).isEqualTo(UUID.fromString(JTI));
    }

    @Test
    @DisplayName("Access apresentado como refresh é refresh inválido")
    void rejectsAccessTokenUse() {
        final var token = JwtTestTokens.token(JwtTestTokens.KEY, JTI, TokenUse.ACCESS.value(),
                JwtPropertiesFixture.ISSUER, List.of(JwtPropertiesFixture.AUDIENCE), NOW, NOW.plusSeconds(3600));

        assertThat(catchThrowable(() -> decoder.refreshId(token)))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    @DisplayName("Texto malformado é refresh inválido")
    void rejectsMalformedToken() {
        assertThat(catchThrowable(() -> decoder.refreshId("nao-e-um-jwt")))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

}
