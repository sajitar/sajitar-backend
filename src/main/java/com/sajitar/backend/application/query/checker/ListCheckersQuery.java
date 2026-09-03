package com.sajitar.backend.application.query.checker;

import java.util.UUID;

import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.port.checker.CheckerPageCriteria;
import com.sajitar.backend.domain.validation.Limit;

import jakarta.validation.constraints.NotNull;

public record ListCheckersQuery(
        @NotNull(message = "{validation.not-null}") UUID profileId,
        @Limit Integer limit,
        @NotNull Boolean reverse,
        Checker.Type lastSeenType) {

    public boolean hasCursor() {
        return lastSeenType != null;
    }

    public CheckerPageCriteria toCriteria() {
        return new CheckerPageCriteria(profileId, lastSeenType, limit, reverse);
    }

}
