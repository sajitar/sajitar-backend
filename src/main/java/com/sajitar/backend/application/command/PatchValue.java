package com.sajitar.backend.application.command;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class PatchValue<T> {

    private final boolean present;

    private final T value;

    public static <T> PatchValue<T> absent() {
        return new PatchValue<>(false, null);
    }

    public static <T> PatchValue<T> of(final T value) {
        return new PatchValue<>(true, value);
    }

    public boolean isPresent() {
        return present;
    }

    public T orElse(final T fallback) {
        return present ? value : fallback;
    }

    @Override
    public String toString() {
        return present ? "PatchValue[" + value + "]" : "PatchValue.absent";
    }

}
