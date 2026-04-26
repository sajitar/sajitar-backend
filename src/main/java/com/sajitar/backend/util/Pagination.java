package com.sajitar.backend.util;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;

@With
@Builder(toBuilder = true)
@RequiredArgsConstructor
public class Pagination<T> {

    @Getter(onMethod_ = @JsonProperty(index = 0))
    private final List<T> content;

    @Getter(onMethod_ = @JsonProperty(index = 1))
    private final long precedingElements;

    @Getter(onMethod_ = @JsonProperty(index = 2))
    private final long followingElements;

    @Getter(onMethod_ = @JsonProperty(index = 3))
    private final boolean reverse;

    @JsonIgnore
    public boolean isEmpty() {
        return content == null;
    }

}
