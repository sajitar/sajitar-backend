package com.sajitar.backend.domain.model.token;

import java.util.UUID;

public record ActiveSession(UUID id, Client client) {
}
