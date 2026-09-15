package com.sajitar.backend.adapter.out.security;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtAudienceValidator;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import com.sajitar.backend.configuration.JwtProperties;
import com.sajitar.backend.domain.model.token.TokenUse;

/**
 * Base dos decoders de access e de refresh. Cada subclasse monta o seu próprio
 * conjunto de validações: o algoritmo fica pinado em HS256 (o {@code alg} do
 * header não decide nada) e {@code iss}, {@code aud}, {@code exp} e
 * {@code token_use} são exigidos.
 */
abstract class NimbusTokenDecoder {

    private final JwtDecoder decoder;

    protected NimbusTokenDecoder(final SecretKey jwtSecretKey, final JwtProperties properties, final TokenUse use) {
        final var nimbus = NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
        nimbus.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                new JwtIssuerValidator(properties.issuer()),
                new JwtAudienceValidator(properties.audience()),
                new JwtClaimValidator<Instant>(JwtClaimNames.EXP, Objects::nonNull),
                new JwtClaimValidator<String>(JwtClaims.TOKEN_USE, use.value()::equals)));
        this.decoder = nimbus;
    }

    protected final Optional<UUID> tokenId(final String token) {
        try {
            return Optional.ofNullable(decoder.decode(token).getId()).map(UUID::fromString);
        } catch (JwtException | IllegalArgumentException _) {
            return Optional.empty();
        }
    }

}
