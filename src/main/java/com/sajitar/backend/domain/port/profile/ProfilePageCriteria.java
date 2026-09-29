package com.sajitar.backend.domain.port.profile;

import java.util.UUID;

import com.sajitar.backend.domain.model.profile.Profile;

public record ProfilePageCriteria(
        String nameContains,
        String lastSeenName,
        UUID lastSeenId,
        int limit,
        boolean reverse,
        boolean includeUnverified,
        boolean includeReaders,
        UUID viewerProfileId,
        Profile.Type type,
        Boolean verified) {

    public boolean hasNameFilter() {
        return nameContains != null && !nameContains.isBlank();
    }

    public boolean hasCursor() {
        return lastSeenName != null && !lastSeenName.isBlank() && lastSeenId != null;
    }

    public ProfilePageCriteria withCursor(final String lastSeenName, final UUID lastSeenId, final boolean reverse) {
        return new ProfilePageCriteria(
                nameContains,
                lastSeenName,
                lastSeenId,
                limit,
                reverse,
                includeUnverified,
                includeReaders,
                viewerProfileId,
                type,
                verified);
    }

}
