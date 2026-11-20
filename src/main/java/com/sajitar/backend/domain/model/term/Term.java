package com.sajitar.backend.domain.model.term;

import java.util.Objects;
import java.util.UUID;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import com.sajitar.backend.domain.exception.InvalidTermLangException;
import com.sajitar.backend.domain.exception.InvalidTermTypeException;

import lombok.With;

public record Term(
        UUID id,
        Term.Type type,
        @With String content,
        @With boolean draft) {

    private static final TimeBasedEpochGenerator ID_GENERATOR = Generators.timeBasedEpochGenerator();

    public static Term create(final Type type, final String content) {
        return new Term(ID_GENERATOR.generate(), type, content, true);
    }

    @Override
    public boolean equals(final Object object) {
        return object instanceof final Term other && Objects.equals(id, other.id) && type == other.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type);
    }

    public enum Type {

        EN,
        PT,
        ES;

        public static Type parse(final String raw) {
            if (raw == null) {
                throw new InvalidTermTypeException("null");
            }
            try {
                return Type.valueOf(raw);
            } catch (final IllegalArgumentException _) {
                throw new InvalidTermTypeException(raw);
            }
        }

        public static Type fromLang(final String lang) {
            if (lang == null) {
                throw new InvalidTermLangException("null");
            }
            return switch (lang) {
                case "en" -> EN;
                case "pt" -> PT;
                case "es" -> ES;
                default -> throw new InvalidTermLangException(lang);
            };
        }

    }

}
