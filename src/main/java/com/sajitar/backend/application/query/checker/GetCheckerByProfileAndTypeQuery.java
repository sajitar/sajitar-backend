package com.sajitar.backend.application.query.checker;

import java.util.UUID;

import com.sajitar.backend.domain.model.checker.Checker;

import jakarta.validation.constraints.NotNull;

public record GetCheckerByProfileAndTypeQuery(
        @NotNull(message = "{validation.not-null}") UUID profileId,
        @NotNull(message = "{validation.not-null}") Checker.Type type) {

}
