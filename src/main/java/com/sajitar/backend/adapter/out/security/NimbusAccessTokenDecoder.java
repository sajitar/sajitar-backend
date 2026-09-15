package com.sajitar.backend.adapter.out.security;

import java.util.Optional;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.sajitar.backend.configuration.JwtProperties;
import com.sajitar.backend.domain.model.token.TokenUse;
import com.sajitar.backend.domain.port.token.AccessTokenDecoder;

@Component
class NimbusAccessTokenDecoder extends NimbusTokenDecoder implements AccessTokenDecoder {

    NimbusAccessTokenDecoder(final SecretKey jwtSecretKey, final JwtProperties properties) {
        super(jwtSecretKey, properties, TokenUse.ACCESS);
    }

    @Override
    public Optional<UUID> accessId(final String token) {
        return tokenId(token);
    }

}
