package com.sajitar.backend.application.command;

import java.util.Objects;

public final class PatchValue<T> {

    private final boolean present;

    private final T value;

    private PatchValue(final boolean present, final T value) {
        this.present = present;
        this.value = value;
    }

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
    public boolean equals(final Object object) {
        return object instanceof final PatchValue<?> other
                && present == other.present
                && Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(present, value);
    }

    @Override
    public String toString() {
        return present ? "PatchValue[" + value + "]" : "PatchValue.absent";
    }

}
