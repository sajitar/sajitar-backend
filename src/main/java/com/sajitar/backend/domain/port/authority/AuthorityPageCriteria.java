package com.sajitar.backend.domain.port.authority;

import java.util.UUID;

import com.sajitar.backend.domain.model.authority.Authority;

public record AuthorityPageCriteria(UUID profileId, Authority.Type lastSeenType, int limit, boolean reverse) {

    public boolean hasCursor() {
        return lastSeenType != null;
    }

    public AuthorityPageCriteria withCursor(final Authority.Type lastSeenType, final boolean reverse) {
        return new AuthorityPageCriteria(profileId, lastSeenType, limit, reverse);
    }

}
