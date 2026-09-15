package com.sajitar.backend.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sajitar.backend.configuration.JwtPropertiesFixture;
import com.sajitar.backend.domain.model.token.TokenUse;

@DisplayName("NimbusAccessTokenDecoder")
class NimbusAccessTokenDecoderTest {

    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.SECONDS);

    private static final String JTI = "018f3c2a-7b00-7c3d-9e1a-000000000001";

    private final NimbusAccessTokenDecoder decoder = new NimbusAccessTokenDecoder(
            JwtTestTokens.KEY,
            JwtPropertiesFixture.defaults());

    @Test
    @DisplayName("Devolve o jti de um access válido")
    void acceptsValidAccessToken() {
        final var token = access(JTI, JwtPropertiesFixture.ISSUER, List.of(JwtPropertiesFixture.AUDIENCE),
                NOW.plusSeconds(3600));

        assertThat(decoder.accessId(token)).contains(UUID.fromString(JTI));
    }

    @Test
    @DisplayName("Rejeita refresh apresentado como access")
    void rejectsRefreshTokenUse() {
        final var token = JwtTestTokens.token(JwtTestTokens.KEY, JTI, TokenUse.REFRESH.value(),
                JwtPropertiesFixture.ISSUER, List.of(JwtPropertiesFixture.AUDIENCE), NOW, NOW.plusSeconds(3600));

        assertThat(decoder.accessId(token)).isEmpty();
    }

    @Test
    @DisplayName("Rejeita token sem a claim token_use")
    void rejectsMissingTokenUse() {
        final var token = JwtTestTokens.token(JwtTestTokens.KEY, JTI, null, JwtPropertiesFixture.ISSUER,
                List.of(JwtPropertiesFixture.AUDIENCE), NOW, NOW.plusSeconds(3600));

        assertThat(decoder.accessId(token)).isEmpty();
    }

    @Test
    @DisplayName("Rejeita issuer de outro emissor")
    void rejectsForeignIssuer() {
        final var token = access(JTI, "outro-emissor", List.of(JwtPropertiesFixture.AUDIENCE), NOW.plusSeconds(3600));

        assertThat(decoder.accessId(token)).isEmpty();
    }

    @Test
    @DisplayName("Rejeita audience diferente da configurada")
    void rejectsForeignAudience() {
        final var token = access(JTI, JwtPropertiesFixture.ISSUER, List.of("outra-app"), NOW.plusSeconds(3600));

        assertThat(decoder.accessId(token)).isEmpty();
    }

    @Test
    @DisplayName("Rejeita token sem audience")
    void rejectsMissingAudience() {
        final var token = access(JTI, JwtPropertiesFixture.ISSUER, null, NOW.plusSeconds(3600));

        assertThat(decoder.accessId(token)).isEmpty();
    }

    @Test
    @DisplayName("Rejeita token sem exp")
    void rejectsMissingExpiration() {
        final var token = access(JTI, JwtPropertiesFixture.ISSUER, List.of(JwtPropertiesFixture.AUDIENCE), null);

        assertThat(decoder.accessId(token)).isEmpty();
    }

    @Test
    @DisplayName("Rejeita token expirado")
    void rejectsExpiredToken() {
        final var token = JwtTestTokens.token(
                JwtTestTokens.KEY,
                JTI,
                TokenUse.ACCESS.value(),
                JwtPropertiesFixture.ISSUER,
                List.of(JwtPropertiesFixture.AUDIENCE),
                NOW.minusSeconds(7200),
                NOW.minusSeconds(3600));

        assertThat(decoder.accessId(token)).isEmpty();
    }

    @Test
    @DisplayName("Rejeita token sem jti")
    void rejectsMissingIdentifier() {
        final var token = access(null, JwtPropertiesFixture.ISSUER, List.of(JwtPropertiesFixture.AUDIENCE),
                NOW.plusSeconds(3600));

        assertThat(decoder.accessId(token)).isEmpty();
    }

    @Test
    @DisplayName("Rejeita jti que não é UUID")
    void rejectsNonUuidIdentifier() {
        final var token = access("nao-e-uuid", JwtPropertiesFixture.ISSUER, List.of(JwtPropertiesFixture.AUDIENCE),
                NOW.plusSeconds(3600));

        assertThat(decoder.accessId(token)).isEmpty();
    }

    @Test
    @DisplayName("Rejeita assinatura de outra chave")
    void rejectsForeignSignature() {
        final var token = JwtTestTokens.token(
                JwtTestTokens.key("outro-segredo-de-32-bytes-012345"),
                JTI,
                TokenUse.ACCESS.value(),
                JwtPropertiesFixture.ISSUER,
                List.of(JwtPropertiesFixture.AUDIENCE),
                NOW,
                NOW.plusSeconds(3600));

        assertThat(decoder.accessId(token)).isEmpty();
    }

    @Test
    @DisplayName("Rejeita texto que nem é JWT")
    void rejectsMalformedToken() {
        assertThat(decoder.accessId("nao-e-um-jwt")).isEmpty();
    }

    private static String access(
            final String id,
            final String issuer,
            final List<String> audience,
            final Instant expiresAt) {
        return JwtTestTokens.token(JwtTestTokens.KEY, id, TokenUse.ACCESS.value(), issuer, audience, NOW, expiresAt);
    }

}
