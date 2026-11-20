package com.sajitar.backend.domain.exception;

import java.util.List;
import java.util.Map;

public final class InvalidTermDraftException extends DomainException {

    public static final String CLOSED_KEY = "validation.term.draft.closed";

    public static final String INCOMPLETE_KEY = "validation.term.draft.incomplete";

    public static final String NULL_KEY = "validation.not-null";

    private InvalidTermDraftException(final String key) {
        super(Map.of("draft", List.of(key)));
    }

    public static InvalidTermDraftException closed() {
        return new InvalidTermDraftException(CLOSED_KEY);
    }

    public static InvalidTermDraftException incomplete() {
        return new InvalidTermDraftException(INCOMPLETE_KEY);
    }

    public static InvalidTermDraftException nullValue() {
        return new InvalidTermDraftException(NULL_KEY);
    }

}
