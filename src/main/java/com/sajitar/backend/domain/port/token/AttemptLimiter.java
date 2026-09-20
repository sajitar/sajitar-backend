package com.sajitar.backend.domain.port.token;

import java.time.Duration;
import java.util.Optional;

import com.sajitar.backend.domain.model.token.AttemptScope;

public interface AttemptLimiter {

    /**
     * Conta mais uma tentativa na janela do escopo. Vazio quando ainda cabe;
     * a espera restante quando o teto já estourou.
     */
    Optional<Duration> register(AttemptScope scope, String key);

}
