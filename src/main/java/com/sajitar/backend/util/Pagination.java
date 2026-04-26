package com.sajitar.backend.util;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Builder;
import lombok.With;

@With
@Builder(toBuilder = true)
public record Pagination<T>(List<T> content, long precedingElements, long followingElements, boolean reverse) {

    @JsonIgnore
    public boolean isEmpty() {
        return Objects.isNull(content);
    }

}
