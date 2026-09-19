package com.sajitar.backend.adapter.in.web.contract.token;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Sessions", description = "Sessões de login ativas do perfil, da mais antiga para a mais recente.")
public record SessionsResponse(
        @Schema(description = "Sessões ativas")
        List<SessionResponse> content) {

    public static SessionsResponse from(final List<UUID> ids, final UUID currentId) {
        return new SessionsResponse(ids.stream()
                .map(id -> new SessionResponse(id, id.equals(currentId)))
                .toList());
    }

}
