package com.sajitar.backend.adapter.out.security;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import com.sajitar.backend.configuration.JwtProperties;
import com.sajitar.backend.domain.model.token.IssuedToken;
import com.sajitar.backend.domain.model.token.TokenClaims;
import com.sajitar.backend.domain.model.token.TokenUse;
import com.sajitar.backend.domain.port.token.TokenIssuer;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class NimbusTokenIssuer implements TokenIssuer {

    private final JwtEncoder jwtEncoder;

    private final JwtProperties properties;

    private final Clock clock;

    @Override
    public IssuedToken issueAccess(final Instant issuedAt) {
        final var claims = TokenClaims.create(
                TokenUse.ACCESS,
                issuedAt,
                issuedAt.plusSeconds(properties.expirationSeconds()));
        return sign(claims, issuedAt);
    }

    @Override
    public IssuedToken issueRefresh(final Instant issuedAt, final Instant sessionBornAt) {
        final var idle = issuedAt.plusSeconds(properties.refreshExpirationSeconds());
        final var absolute = sessionBornAt.plusSeconds(properties.sessionMaxSeconds());
        final var claims = TokenClaims.create(TokenUse.REFRESH, issuedAt, idle.isBefore(absolute) ? idle : absolute);
        return sign(claims, issuedAt);
    }

    @Override
    public IssuedToken reissue(final TokenClaims claims) {
        return sign(claims, clock.instant());
    }

    private IssuedToken sign(final TokenClaims claims, final Instant now) {
        final var claimsSet = JwtClaimsSet.builder()
                .id(claims.id().toString())
                .issuer(properties.issuer())
                .audience(List.of(properties.audience()))
                .issuedAt(claims.issuedAt())
                .expiresAt(claims.expiresAt())
                .claim(JwtClaims.TOKEN_USE, claims.use().value())
                .build();
        final var header = JwsHeader.with(MacAlgorithm.HS256).build();
        final var value = jwtEncoder.encode(JwtEncoderParameters.from(header, claimsSet)).getTokenValue();
        return new IssuedToken(claims, value, claims.expiresInSeconds(now));
    }

}
