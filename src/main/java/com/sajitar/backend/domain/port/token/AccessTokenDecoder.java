package com.sajitar.backend.domain.port.token;

import java.util.Optional;
import java.util.UUID;

public interface AccessTokenDecoder {

    /**
     * Devolve o {@code jti} do access quando assinatura, {@code iss}, {@code aud},
     * {@code token_use} e {@code exp} conferem; vazio em qualquer outro caso.
     */
    Optional<UUID> accessId(String token);

}
