package com.sajitar.backend.adapter.out.security;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import com.sajitar.backend.configuration.JwtProperties;
import com.sajitar.backend.domain.port.AccessToken;
import com.sajitar.backend.domain.port.AccessTokenIssuer;
import com.sajitar.backend.domain.port.TokenPair;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class NimbusAccessTokenIssuer implements AccessTokenIssuer {

    private final JwtEncoder jwtEncoder;

    private final JwtProperties properties;

    private final Clock clock;

    @Override
    public TokenPair issue(final UUID profileId) {
        final var now = clock.instant();
        return new TokenPair(
                encode(profileId, now, properties.expirationSeconds(), JwtTokenUse.ACCESS),
                encode(profileId, now, properties.refreshExpirationSeconds(), JwtTokenUse.REFRESH));
    }

    private AccessToken encode(
            final UUID profileId,
            final Instant now,
            final int expiresInSeconds,
            final String tokenUse) {
        final var claims = JwtClaimsSet.builder()
                .subject(profileId.toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiresInSeconds))
                .claim(JwtTokenUse.CLAIM, tokenUse)
                .build();
        final var header = JwsHeader.with(MacAlgorithm.HS256).build();
        final var token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(token, expiresInSeconds);
    }

}
