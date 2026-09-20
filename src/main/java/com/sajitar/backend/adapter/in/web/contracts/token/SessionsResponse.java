package com.sajitar.backend.adapter.in.web.contracts.token;

import java.util.List;
import java.util.UUID;

import com.sajitar.backend.domain.model.token.ActiveSession;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Sessions", description = "Sessões de login ativas do perfil, da mais antiga para a mais recente.")
public record SessionsResponse(
        @Schema(description = "Sessões ativas")
        List<SessionResponse> content) {

    public static SessionsResponse from(final List<ActiveSession> sessions, final UUID currentId) {
        return new SessionsResponse(sessions.stream()
                .map(session -> new SessionResponse(
                        session.id(),
                        session.id().equals(currentId),
                        ClientResponse.from(session.client())))
                .toList());
    }

}
