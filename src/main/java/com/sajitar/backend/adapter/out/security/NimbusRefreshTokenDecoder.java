package com.sajitar.backend.adapter.out.security;

import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.sajitar.backend.configuration.JwtProperties;
import com.sajitar.backend.domain.exception.InvalidRefreshTokenException;
import com.sajitar.backend.domain.model.token.TokenUse;
import com.sajitar.backend.domain.port.token.RefreshTokenDecoder;

@Component
class NimbusRefreshTokenDecoder extends NimbusTokenDecoder implements RefreshTokenDecoder {

    NimbusRefreshTokenDecoder(final SecretKey jwtSecretKey, final JwtProperties properties) {
        super(jwtSecretKey, properties, TokenUse.REFRESH);
    }

    @Override
    public UUID refreshId(final String token) {
        return tokenId(token).orElseThrow(InvalidRefreshTokenException::new);
    }

}
