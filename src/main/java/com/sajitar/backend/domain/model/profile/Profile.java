package com.sajitar.backend.domain.model.profile;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;

public record Profile(
        UUID id,
        String name,
        String description,
        LocalDate birthday,
        String email,
        String password) {

    private static final TimeBasedEpochGenerator ID_GENERATOR = Generators.timeBasedEpochGenerator();

    public static Profile create(
            final String name,
            final String description,
            final LocalDate birthday,
            final String email,
            final String password) {
        return new Profile(ID_GENERATOR.generate(), name, description, birthday, email, password);
    }

    public Profile withId(final UUID id) {
        return new Profile(id, name, description, birthday, email, password);
    }

    public Profile withName(final String name) {
        return new Profile(id, name, description, birthday, email, password);
    }

    public Profile withEmail(final String email) {
        return new Profile(id, name, description, birthday, email, password);
    }

    public Profile withPassword(final String password) {
        return new Profile(id, name, description, birthday, email, password);
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
