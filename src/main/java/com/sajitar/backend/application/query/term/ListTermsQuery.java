package com.sajitar.backend.application.query.term;

import java.util.UUID;

import com.sajitar.backend.domain.model.term.TermDetail;
import com.sajitar.backend.domain.port.term.TermPageCriteria;
import com.sajitar.backend.domain.validation.Limit;

import jakarta.validation.constraints.NotNull;

public record ListTermsQuery(
        TermDetail.Type type,
        UUID lastSeenId,
        @Limit Integer limit,
        @NotNull(message = "{validation.not-null}") Boolean reverse) {

    public boolean hasCursor() {
        return lastSeenId != null;
    }

    public TermPageCriteria toCriteria() {
        return new TermPageCriteria(type, lastSeenId, limit, reverse);
    }

}
