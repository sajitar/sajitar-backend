package com.sajitar.backend.domain.port.checker;

import java.util.UUID;

import com.sajitar.backend.domain.model.checker.Checker;

public record CheckerPageCriteria(UUID profileId, Checker.Type lastSeenType, int limit, boolean reverse) {

    public boolean hasCursor() {
        return lastSeenType != null;
    }

    public CheckerPageCriteria withCursor(final Checker.Type lastSeenType, final boolean reverse) {
        return new CheckerPageCriteria(profileId, lastSeenType, limit, reverse);
    }

}
