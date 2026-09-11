package com.sajitar.backend.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.sajitar.backend.domain.exception.InvalidRefreshTokenException;

@DisplayName("NimbusRefreshTokenParser")
class NimbusRefreshTokenParserTest {

    private static final String SECRET = "01234567890123456789012345678901";

    private static final UUID PROFILE_ID = UUID.fromString("01989bad-6161-7000-0ae9-f440b10578ec");

    private final SecretKey key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

    private final JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));

    private final NimbusRefreshTokenParser parser = new NimbusRefreshTokenParser(key);

    @Test
    @DisplayName("Devolve o sub quando o JWT tem token_use refresh")
    void returnsSubjectWhenTokenUseIsRefresh() {
        final var token = encode(PROFILE_ID.toString(), JwtTokenUse.REFRESH, Instant.now().plusSeconds(604800));

        assertThat(parser.profileId(token)).isEqualTo(PROFILE_ID);
    }

    @Test
    @DisplayName("Rejeita JWT de access usado como refresh")
    void rejectsAccessToken() {
        final var token = encode(PROFILE_ID.toString(), JwtTokenUse.ACCESS, Instant.now().plusSeconds(3600));

        final var thrown = catchThrowable(() -> parser.profileId(token));

        assertThat(thrown).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    @DisplayName("Rejeita token malformado")
    void rejectsGarbage() {
        final var thrown = catchThrowable(() -> parser.profileId("not-a-jwt"));

        assertThat(thrown).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    @DisplayName("Rejeita refresh cujo sub não é UUID")
    void rejectsNonUuidSubject() {
        final var token = encode("not-a-uuid", JwtTokenUse.REFRESH, Instant.now().plusSeconds(604800));

        final var thrown = catchThrowable(() -> parser.profileId(token));

        assertThat(thrown).isInstanceOf(InvalidRefreshTokenException.class);
    }

    private String encode(final String subject, final String tokenUse, final Instant expiresAt) {
        final var now = Instant.now();
        final var claims = JwtClaimsSet.builder()
                .subject(subject)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(JwtTokenUse.CLAIM, tokenUse)
                .build();
        final var header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

}
