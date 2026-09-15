package com.sajitar.backend.domain.port.token;

import java.util.UUID;

import com.sajitar.backend.domain.model.token.TokenClaims;

public sealed interface RotationOutcome {

    /** O refresh apresentado era o vigente e o par novo já está gravado. */
    record Rotated() implements RotationOutcome {
    }

    /**
     * Retry dentro da janela de graça: nada foi gravado e o par sucessor da
     * rotação anterior deve ser reassinado a partir destas claims.
     */
    record Replayed(UUID sessionId, TokenClaims access, TokenClaims refresh) implements RotationOutcome {
    }

    /**
     * Refresh sem registro vigente, órfão ou reapresentado fora da graça. No
     * último caso a sessão já foi apagada antes deste retorno.
     */
    record Invalid() implements RotationOutcome {
    }

}
