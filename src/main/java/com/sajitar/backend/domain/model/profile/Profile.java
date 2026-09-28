package com.sajitar.backend.domain.model.profile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import com.sajitar.backend.domain.exception.InvalidProfileTypeException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;
import lombok.experimental.Accessors;

public record Profile(
        @With UUID id,
        @With Type type,
        @With String name,
        String description,
        LocalDate birthday,
        @With String email,
        @With String password) {

    private static final TimeBasedEpochGenerator ID_GENERATOR = Generators.timeBasedEpochGenerator();

    public static Profile create(
            final Type type,
            final String name,
            final String description,
            final LocalDate birthday,
            final String email,
            final String password) {
        return new Profile(ID_GENERATOR.generate(), type, name, description, birthday, email, password);
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

    @Getter
    @Accessors(fluent = true)
    @RequiredArgsConstructor
    public enum Type {

        MASTER(0),
        WRITER(1),
        READER(2);

        private final int value;

        public boolean includes(final Type required) {
            return value <= required.value;
        }

        public static Type valueOf(final int value) {
            return switch (value) {
                case 0 -> MASTER;
                case 1 -> WRITER;
                case 2 -> READER;
                default -> throw new InvalidProfileTypeException(Integer.toString(value));
            };
        }

        public static Type parse(final String raw) {
            if (raw == null) {
                throw new InvalidProfileTypeException("null");
            }
            for (final var type : values()) {
                if (type.name().equals(raw)) {
                    return type;
                }
            }
            try {
                return valueOf(Integer.parseInt(raw));
            } catch (final NumberFormatException _) {
                throw new InvalidProfileTypeException(raw);
            }
        }

    }

}
