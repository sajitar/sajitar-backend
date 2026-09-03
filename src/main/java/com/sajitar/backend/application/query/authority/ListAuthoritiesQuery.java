package com.sajitar.backend.application.query.authority;

import java.util.UUID;

import com.sajitar.backend.domain.model.authority.Authority;
import com.sajitar.backend.domain.port.authority.AuthorityPageCriteria;
import com.sajitar.backend.domain.validation.Limit;

import jakarta.validation.constraints.NotNull;

public record ListAuthoritiesQuery(
        @NotNull(message = "{validation.not-null}") UUID profileId,
        @Limit Integer limit,
        @NotNull Boolean reverse,
        Authority.Type lastSeenType) {

    public boolean hasCursor() {
        return lastSeenType != null;
    }

    public AuthorityPageCriteria toCriteria() {
        return new AuthorityPageCriteria(profileId, lastSeenType, limit, reverse);
    }

}
