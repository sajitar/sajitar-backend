package com.sajitar.backend.domain.model.token;

import java.util.UUID;

public record IssuedSession(UUID sessionId, IssuedToken access, IssuedToken refresh) {

    public boolean hasRefresh() {
        return refresh != null;
    }

}
