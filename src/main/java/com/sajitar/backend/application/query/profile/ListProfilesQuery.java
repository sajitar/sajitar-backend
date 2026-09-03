package com.sajitar.backend.application.query.profile;

import com.sajitar.backend.domain.port.profile.ProfilePageCriteria;
import com.sajitar.backend.domain.validation.Limit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ListProfilesQuery(
        @Limit Integer limit,
        @NotNull Boolean reverse,
        String name,
        @Valid ProfileCursor cursor) {

    public boolean hasNameFilter() {
        return name != null && !name.isBlank();
    }

    public boolean hasCursor() {
        return cursor != null;
    }

    public ProfilePageCriteria toCriteria() {
        return new ProfilePageCriteria(
                hasNameFilter() ? name : null,
                hasCursor() ? cursor.lastSeenName() : null,
                hasCursor() ? cursor.lastSeenId() : null,
                limit,
                reverse);
    }

}
