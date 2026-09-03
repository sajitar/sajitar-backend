package com.sajitar.backend.application.query.profile;

import java.util.UUID;

import com.sajitar.backend.domain.validation.profile.Name;

import jakarta.validation.constraints.NotNull;

public record ProfileCursor(
        @Name String lastSeenName,
        @NotNull UUID lastSeenId) {

}
