package com.sajitar.backend.application.query;

import java.util.UUID;

import com.sajitar.backend.domain.model.Checker;
import com.sajitar.backend.domain.port.CheckerPageCriteria;
import com.sajitar.backend.domain.validation.Limit;

import jakarta.validation.constraints.NotNull;

public record ListCheckersQuery(
        @NotNull(message = "{validation.not-null}") UUID profileId,
        @Limit Integer limit,
        Checker.Type lastSeenType) {

    public CheckerPageCriteria toCriteria() {
        return new CheckerPageCriteria(profileId, lastSeenType, limit);
    }

}
