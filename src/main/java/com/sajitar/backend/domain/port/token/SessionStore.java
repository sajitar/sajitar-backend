package com.sajitar.backend.domain.port.token;

import java.util.Optional;
import java.util.UUID;

import com.sajitar.backend.domain.model.token.Session;
import com.sajitar.backend.domain.model.token.TokenClaims;

public interface SessionStore {

    /**
     * Grava a sessão e seus tokens. Quando o perfil estoura o teto de sessões
     * ativas, a mais antiga é encerrada antes da gravação.
     */
    void open(Session session, TokenClaims access, TokenClaims refresh);

    /** Perfil do access ativo e vigente na sessão; vazio se não houver registro. */
    Optional<UUID> profileIdOfActiveAccess(UUID accessId);

    /** Sessão cujo refresh vigente é o {@code jti} informado. */
    Optional<Session> findActiveRefresh(UUID refreshId);

    /**
     * Troca o refresh vigente pelo par novo em uma única operação atômica. Se o
     * {@code jti} apresentado já tiver sido consumido, reemite o sucessor dentro
     * da graça ou apaga a sessão fora dela.
     */
    RotationOutcome rotate(RotationCommand command);

    /** Mesmo tratamento de refresh consumido, quando não há par candidato. */
    RotationOutcome replay(UUID refreshId);

}
