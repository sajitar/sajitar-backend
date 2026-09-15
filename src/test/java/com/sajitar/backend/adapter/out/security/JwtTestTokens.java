package com.sajitar.backend.adapter.out.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.sajitar.backend.configuration.JwtPropertiesFixture;

/** Emite tokens arbitrários (inclusive inválidos) para exercitar os decoders. */
final class JwtTestTokens {

    static final SecretKey KEY = key(JwtPropertiesFixture.SECRET);

    private JwtTestTokens() {
    }

    static SecretKey key(final String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    static JwtEncoder encoder(final SecretKey key) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    static String token(
            final SecretKey key,
            final String id,
            final String tokenUse,
            final String issuer,
            final List<String> audience,
            final Instant issuedAt,
            final Instant expiresAt) {
        final var claims = JwtClaimsSet.builder().issuer(issuer).issuedAt(issuedAt);
        if (id != null) {
            claims.id(id);
        }
        if (audience != null) {
            claims.audience(audience);
        }
        if (expiresAt != null) {
            claims.expiresAt(expiresAt);
        }
        if (tokenUse != null) {
            claims.claim(JwtClaims.TOKEN_USE, tokenUse);
        }
        return encoder(key)
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build()))
                .getTokenValue();
    }

}
