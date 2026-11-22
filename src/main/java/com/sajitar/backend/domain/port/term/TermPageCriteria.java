package com.sajitar.backend.domain.port.term;

import java.util.UUID;

import com.sajitar.backend.domain.model.term.Term;

public record TermPageCriteria(Term.Type type, UUID lastSeenId, int limit, boolean reverse) {

    public boolean hasCursor() {
        return lastSeenId != null;
    }

    public boolean hasTypeFilter() {
        return type != null;
    }

    public TermPageCriteria withCursor(final UUID lastSeenId, final boolean reverse) {
        return new TermPageCriteria(type, lastSeenId, limit, reverse);
    }

}
