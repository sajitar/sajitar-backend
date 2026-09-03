package com.sajitar.backend.application.query;

import java.util.UUID;

import com.sajitar.backend.domain.model.Checker;

import jakarta.validation.constraints.NotNull;

public record GetCheckerByProfileAndTypeQuery(
        @NotNull(message = "{validation.not-null}") UUID profileId,
        @NotNull(message = "{validation.not-null}") Checker.Type type) {

}
