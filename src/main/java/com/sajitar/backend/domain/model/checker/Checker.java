package com.sajitar.backend.domain.model.checker;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import com.sajitar.backend.domain.exception.InvalidCheckerTypeException;

public record Checker(
        UUID id,
        UUID profileId,
        Type type,
        String code,
        String payload,
        int attempts,
        int replaces,
        Instant updatedAt) {

    public static final int ATTEMPTS_MAX = 10;

    public static final int ATTEMPTS_MIN = 0;

    public static final int REPLACES_MAX = 3;

    public static final int REPLACES_MIN = 0;

    public static final int CODE_LENGTH = 6;

    private static final TimeBasedEpochGenerator ID_GENERATOR = Generators.timeBasedEpochGenerator();

    public static Checker create(final UUID profileId, final Type type) {
        return new Checker(
                ID_GENERATOR.generate(),
                profileId,
                type,
                newCode(),
                null,
                ATTEMPTS_MAX,
                REPLACES_MAX,
                Instant.now());
    }

    public static String newCode() {
        final var random = new SecureRandom();
        final var digits = new char[CODE_LENGTH];
        for (int i = 0; i < CODE_LENGTH; i++) {
            digits[i] = (char) ('0' + random.nextInt(10));
        }
        return new String(digits);
    }

    public Checker withCode(final String code) {
        return new Checker(id, profileId, type, code, payload, attempts, replaces, updatedAt);
    }

    public Checker withPayload(final String payload) {
        return new Checker(id, profileId, type, code, payload, attempts, replaces, updatedAt);
    }

    public Checker withAttempts(final int attempts) {
        return new Checker(id, profileId, type, code, payload, attempts, replaces, updatedAt);
    }

    public Checker withReplaces(final int replaces) {
        return new Checker(id, profileId, type, code, payload, attempts, replaces, updatedAt);
    }

    public Checker withUpdatedAt(final Instant updatedAt) {
        return new Checker(id, profileId, type, code, payload, attempts, replaces, updatedAt);
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

    public enum Type {

        CHANGE_EMAIL(0, false),
        VERIFY_EMAIL(1, true),
        CHANGE_PASSWORD(2, false);

        private final int value;

        private final boolean restrict;

        Type(final int value, final boolean restrict) {
            this.value = value;
            this.restrict = restrict;
        }

        public int value() {
            return value;
        }

        public boolean restrict() {
            return restrict;
        }

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
