package com.sajitar.backend.adapter.out.security;

import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import com.sajitar.backend.domain.exception.InvalidRefreshTokenException;
import com.sajitar.backend.domain.port.RefreshTokenParser;

@Component
class NimbusRefreshTokenParser implements RefreshTokenParser {

    private final JwtDecoder decoder;

    NimbusRefreshTokenParser(final SecretKey jwtSecretKey) {
        this.decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Override
    public UUID profileId(final String refreshToken) {
        try {
            final var jwt = decoder.decode(refreshToken);
            if (!JwtTokenUse.REFRESH.equals(jwt.getClaimAsString(JwtTokenUse.CLAIM))) {
                throw new InvalidRefreshTokenException();
            }
            return UUID.fromString(jwt.getSubject());
        } catch (JwtException | IllegalArgumentException _) {
            throw new InvalidRefreshTokenException();
        }
    }

}
