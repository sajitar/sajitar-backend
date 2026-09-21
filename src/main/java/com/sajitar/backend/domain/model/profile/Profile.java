package com.sajitar.backend.domain.model.profile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;

import lombok.With;

public record Profile(
        @With UUID id,
        @With String name,
        String description,
        LocalDate birthday,
        @With String email,
        @With String password) {

    private static final TimeBasedEpochGenerator ID_GENERATOR = Generators.timeBasedEpochGenerator();

    public static Profile create(
            final String name,
            final String description,
            final LocalDate birthday,
            final String email,
            final String password) {
        return new Profile(ID_GENERATOR.generate(), name, description, birthday, email, password);
    }

    public Instant bornAt() {
        return Instant.ofEpochMilli(id.getMostSignificantBits() >>> 16);
    }

    @Override
    public boolean equals(final Object object) {
        return object instanceof final Profile other && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

}
