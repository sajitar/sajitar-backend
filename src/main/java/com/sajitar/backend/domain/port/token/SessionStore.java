package com.sajitar.backend.domain.port.token;

import java.util.List;
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

    /** Sessão cujo access vigente é o {@code jti} informado. */
    Optional<Session> findActiveAccess(UUID accessId);

    /** Sessão cujo refresh vigente é o {@code jti} informado. */
    Optional<Session> findActiveRefresh(UUID refreshId);

    /** Sessões ativas do perfil, da mais antiga para a mais recente. */
    List<UUID> activeSessionIds(UUID profileId);

    /**
     * Encerra as sessões informadas em lote. Nada é encerrado — e o retorno é
     * {@code false} — se algum id não for sessão ativa daquele perfil.
     */
    boolean close(UUID profileId, List<UUID> sessionIds);

    /** Encerra todas as sessões do perfil, usado nos eventos de conta. */
    void wipe(UUID profileId);

    /**
     * Troca o refresh vigente pelo par novo em uma única operação atômica. Se o
     * {@code jti} apresentado já tiver sido consumido, reemite o sucessor dentro
     * da graça ou apaga a sessão fora dela.
     */
    RotationOutcome rotate(RotationCommand command);

    /** Mesmo tratamento de refresh consumido, quando não há par candidato. */
    RotationOutcome replay(UUID refreshId);

}
