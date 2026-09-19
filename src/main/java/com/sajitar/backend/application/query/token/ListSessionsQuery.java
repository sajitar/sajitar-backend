package com.sajitar.backend.application.query.token;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ListSessionsQuery(@NotNull UUID profileId) {
}
