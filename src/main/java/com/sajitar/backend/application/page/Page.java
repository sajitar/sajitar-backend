package com.sajitar.backend.application.page;

import java.util.List;

public record Page<T>(List<T> content, long precedingElements, long followingElements, boolean reverse) {

    public Page {
        content = content == null ? List.of() : List.copyOf(content);
    }

    public static <T> Page<T> empty(final boolean reverse) {
        return new Page<>(List.of(), 0, 0, reverse);
    }

    public boolean isEmpty() {
        return content.isEmpty();
    }

}
