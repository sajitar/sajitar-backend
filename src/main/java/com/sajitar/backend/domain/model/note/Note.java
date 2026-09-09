package com.sajitar.backend.domain.model.note;

import java.util.Objects;
import java.util.UUID;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import com.sajitar.backend.domain.exception.InvalidNoteTypeException;

public record Note(UUID id, UUID profileId, Type type, String content) {

    private static final TimeBasedEpochGenerator ID_GENERATOR = Generators.timeBasedEpochGenerator();

    public static Note create(final UUID profileId, final Type type, final String content) {
        return new Note(ID_GENERATOR.generate(), profileId, type, content);
    }

    public Note withType(final Type type) {
        return new Note(id, profileId, type, content);
    }

    public Note withContent(final String content) {
        return new Note(id, profileId, type, content);
    }

    @Override
    public boolean equals(final Object object) {
        return object instanceof final Note other && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public enum Type {

        PUBLIC(0),
        PROTECTED(1),
        PRIVATE(2);

        private final int value;

        Type(final int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }

        public static Type valueOf(final int value) {
            return switch (value) {
                case 0 -> PUBLIC;
                case 1 -> PROTECTED;
                case 2 -> PRIVATE;
                default -> throw new InvalidNoteTypeException(Integer.toString(value));
            };
        }

        public static Type parse(final String raw) {
            if (raw == null) {
                throw new InvalidNoteTypeException("null");
            }
            for (final var type : values()) {
                if (type.name().equals(raw)) {
                    return type;
                }
            }
            try {
                return valueOf(Integer.parseInt(raw));
            } catch (final NumberFormatException _) {
                throw new InvalidNoteTypeException(raw);
            }
        }

    }

}
