package com.sajitar.backend.domain.port.checker;

import java.util.UUID;

import com.sajitar.backend.domain.model.checker.Checker;

public record CheckerPageCriteria(UUID profileId, Checker.Type lastSeenType, int limit) {

    public boolean hasCursor() {
        return lastSeenType != null;
    }

}
