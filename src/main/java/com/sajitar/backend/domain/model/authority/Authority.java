package com.sajitar.backend.domain.model.authority;

import java.util.Objects;
import java.util.UUID;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import com.sajitar.backend.domain.exception.InvalidAuthorityTypeException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;
import lombok.experimental.Accessors;

public record Authority(
        UUID id,
        UUID profileId,
        @With Authority.Type type) {

    private static final TimeBasedEpochGenerator ID_GENERATOR = Generators.timeBasedEpochGenerator();

    public static Authority create(final UUID profileId, final Type type) {
        return new Authority(ID_GENERATOR.generate(), profileId, type);
    }

    @Override
    public boolean equals(final Object object) {
        return object instanceof final Authority other && Objects.equals(id, other.id);
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
        MEMBER(1),
        READER(2);

        private final int value;

        public static Type valueOf(final int value) {
            return switch (value) {
                case 0 -> MASTER;
                case 1 -> MEMBER;
                case 2 -> READER;
                default -> throw new InvalidAuthorityTypeException(Integer.toString(value));
            };
        }

        public static Type parse(final String raw) {
            if (raw == null) {
                throw new InvalidAuthorityTypeException("null");
            }
            for (final var type : values()) {
                if (type.name().equals(raw)) {
                    return type;
                }
            }
            try {
                return valueOf(Integer.parseInt(raw));
            } catch (final NumberFormatException _) {
                throw new InvalidAuthorityTypeException(raw);
            }
        }

    }

}
