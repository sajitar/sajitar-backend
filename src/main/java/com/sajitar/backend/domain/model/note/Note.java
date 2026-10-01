package com.sajitar.backend.domain.model.note;

import java.util.Objects;
import java.util.UUID;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import com.sajitar.backend.domain.exception.InvalidNoteTypeException;

import lombok.With;

public record Note(
        UUID id,
        UUID profileId,
        @With Note.Type type,
        @With String content) {

    private static final TimeBasedEpochGenerator ID_GENERATOR = Generators.timeBasedEpochGenerator();

    public static Note create(final UUID profileId, final Type type, final String content) {
        return new Note(ID_GENERATOR.generate(), profileId, type, content);
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

        PUBLIC,
        PROTECTED,
        PRIVATE;

        public static Type parse(final String raw) {
            if (raw == null) {
                throw new InvalidNoteTypeException("null");
            }
            try {
                return Type.valueOf(raw);
            } catch (final IllegalArgumentException _) {
                throw new InvalidNoteTypeException(raw);
            }
        }

    }

}
