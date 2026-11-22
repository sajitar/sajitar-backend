package com.sajitar.backend.application.usecase.token;

import java.util.Optional;
import java.util.UUID;

import com.sajitar.backend.domain.exception.InvalidTermIdException;
import com.sajitar.backend.domain.exception.TermRequiredException;
import com.sajitar.backend.domain.model.profile.Profile;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Pattern;
import lombok.experimental.UtilityClass;

@UtilityClass
class TermAcceptance {

    static final String FORMAT_KEY = "{validation.termId.format}";

    static Decision resolve(
            final Profile profile,
            final String raw,
            final Optional<UUID> currentId,
            final Validator validator) {
        if (profile.type().includes(Profile.Type.MASTER)) {
            return Decision.keep();
        }
        if (currentId.isEmpty()) {
            return Decision.store(null);
        }
        final var current = currentId.get();
        if (current.equals(profile.termId())) {
            return Decision.keep();
        }
        if (raw == null || raw.isBlank()) {
            throw new TermRequiredException();
        }
        final var violations = validator.validate(new TermIdText(raw));
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        final var parsed = UUID.fromString(raw);
        if (!parsed.equals(current)) {
            throw new InvalidTermIdException();
        }
        return Decision.store(current);
    }

    record Decision(boolean persist, UUID termId) {

        static Decision keep() {
            return new Decision(false, null);
        }

        static Decision store(final UUID termId) {
            return new Decision(true, termId);
        }

    }

    private record TermIdText(
            @Pattern(
                    regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                    message = FORMAT_KEY)
            String termId) {
    }

}
