package com.sajitar.backend.domain.model.checker;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import com.sajitar.backend.domain.exception.InvalidCheckerTypeException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;
import lombok.experimental.Accessors;

public record Checker(
        UUID id,
        UUID profileId,
        Type type,
        @With String code,
        @With String payload) {

    public static final int CODE_LENGTH = 6;

    private static final TimeBasedEpochGenerator ID_GENERATOR = Generators.timeBasedEpochGenerator();

    public static Checker create(final UUID profileId, final Type type) {
        return new Checker(ID_GENERATOR.generate(), profileId, type, newCode(), null);
    }

    public static UUID uuidV7At(final Instant instant) {
        final var msb = (instant.toEpochMilli() << 16) | 0x7000L;
        return new UUID(msb, 0x8000000000000000L);
    }

    public boolean createdBefore(final Instant cutoff) {
        return id.compareTo(uuidV7At(cutoff)) < 0;
    }

    public static String newCode() {
        final var random = new SecureRandom();
        final var digits = new char[CODE_LENGTH];
        for (int i = 0; i < CODE_LENGTH; i++) {
            digits[i] = (char) ('0' + random.nextInt(10));
        }
        return new String(digits);
    }

    public Checker rotate(final Type type, final String payload) {
        return new Checker(id, profileId, type, newCode(), payload);
    }

    public boolean requiredPayload() {
        return switch (type) {
            case CHANGE_EMAIL -> payload == null;
            case VERIFY_EMAIL -> false;
            case CHANGE_PASSWORD -> true;
        };
    }

    @Override
    public boolean equals(final Object object) {
        return object instanceof final Checker other && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Getter
    @Accessors(fluent = true)
    @RequiredArgsConstructor
    public enum Type {

        CHANGE_EMAIL(0),
        VERIFY_EMAIL(1),
        CHANGE_PASSWORD(2);

        private final int value;

        public static Type valueOf(final int value) {
            return switch (value) {
                case 0 -> CHANGE_EMAIL;
                case 1 -> VERIFY_EMAIL;
                case 2 -> CHANGE_PASSWORD;
                default -> throw new InvalidCheckerTypeException(Integer.toString(value));
            };
        }

        public static Type parse(final String raw) {
            if (raw == null) {
                throw new InvalidCheckerTypeException("null");
            }
            for (final var type : values()) {
                if (type.name().equals(raw)) {
                    return type;
                }
            }
            try {
                return valueOf(Integer.parseInt(raw));
            } catch (final NumberFormatException _) {
                throw new InvalidCheckerTypeException(raw);
            }
        }

    }

}
