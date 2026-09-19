package com.sajitar.backend.application.usecase.token;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.query.token.ListSessionsQuery;
import com.sajitar.backend.domain.port.token.SessionStore;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListSessionsUseCase {

    private final SessionStore sessions;

    private final Validator validator;

    /**
     * Sessões de login ativas do perfil, da mais antiga para a mais recente. O
     * instante do login sai dos 48 bits de tempo de cada id, então a listagem não
     * precisa devolver data alguma.
     */
    public List<UUID> execute(final ListSessionsQuery query) {
        Constraints.requireValid(validator, query);
        return sessions.activeSessionIds(query.profileId());
    }

}
