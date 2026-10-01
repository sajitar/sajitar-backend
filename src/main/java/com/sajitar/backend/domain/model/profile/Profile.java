package com.sajitar.backend.domain.model.profile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import com.sajitar.backend.domain.exception.InvalidProfileTypeException;

import lombok.RequiredArgsConstructor;
import lombok.With;

public record Profile(
        @With UUID id,
        @With Type type,
        @With String name,
        String description,
        LocalDate birthday,
        @With String email,
        @With String password,
        @With boolean twoFactor) {

    private static final TimeBasedEpochGenerator ID_GENERATOR = Generators.timeBasedEpochGenerator();

    public static Profile create(
            final Type type,
            final String name,
            final String description,
            final LocalDate birthday,
            final String email,
            final String password) {
        return new Profile(
                ID_GENERATOR.generate(), type, name, description, birthday, email, password, type.includes(Type.MASTER));
    }

    public Instant bornAt() {
        return Instant.ofEpochMilli(id.getMostSignificantBits() >>> 16);
    }

    public boolean requiresTwoFactor() {
        return twoFactor;
    }

    @Override
    public boolean equals(final Object object) {
        return object instanceof final Profile other && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @RequiredArgsConstructor
    public enum Type {

        MASTER(0),
        WRITER(1),
        READER(2);

        private final transient int level;

        public boolean includes(final Type required) {
            return level <= required.level;
        }

        public static Type parse(final String raw) {
            if (raw == null) {
                throw new InvalidProfileTypeException("null");
            }
            try {
                return Type.valueOf(raw);
            } catch (final IllegalArgumentException _) {
                throw new InvalidProfileTypeException(raw);
            }
        }

    }

}
