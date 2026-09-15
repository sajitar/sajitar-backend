package com.sajitar.backend.domain.port.token;

import java.time.Instant;

import com.sajitar.backend.domain.model.token.IssuedToken;
import com.sajitar.backend.domain.model.token.TokenClaims;

public interface TokenIssuer {

    IssuedToken issueAccess(Instant issuedAt);

    /**
     * Emite um refresh cujo {@code exp} é o menor entre a janela de inatividade e
     * o teto absoluto contado do nascimento da sessão.
     */
    IssuedToken issueRefresh(Instant issuedAt, Instant sessionBornAt);

    /**
     * Reassina claims já emitidas, usado no retry dentro da janela de graça: o
     * compacto não fica no Redis, só as claims do par sucessor.
     */
    IssuedToken reissue(TokenClaims claims);

}
