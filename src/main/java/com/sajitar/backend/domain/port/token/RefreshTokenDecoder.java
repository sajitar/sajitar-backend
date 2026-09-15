package com.sajitar.backend.domain.port.token;

import java.util.UUID;

public interface RefreshTokenDecoder {

    /**
     * Devolve o {@code jti} do refresh; lança
     * {@link com.sajitar.backend.domain.exception.InvalidRefreshTokenException}
     * quando assinatura, {@code iss}, {@code aud}, {@code token_use} ou
     * {@code exp} não conferem.
     */
    UUID refreshId(String token);

}
