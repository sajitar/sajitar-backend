package com.sajitar.backend.domain.model.term;

import java.util.Objects;
import java.util.UUID;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;

import lombok.With;

public record Term(
        UUID id,
        @With String content) {

    private static final TimeBasedEpochGenerator ID_GENERATOR = Generators.timeBasedEpochGenerator();

    public static Term create(final String content) {
        return new Term(ID_GENERATOR.generate(), content);
    }

    @Override
    public boolean equals(final Object object) {
        return object instanceof final Term other && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

}
