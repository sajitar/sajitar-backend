package com.sajitar.backend.application.usecase.profile;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.command.profile.PatchProfileCommand;
import com.sajitar.backend.domain.exception.ForbiddenProfileTypeException;
import com.sajitar.backend.domain.exception.ForbiddenProfileUpdateException;
import com.sajitar.backend.domain.exception.InvalidCredentialsException;
import com.sajitar.backend.domain.exception.ProfileNotFoundException;
import com.sajitar.backend.domain.exception.TooManyAttemptsException;
import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.model.profile.Profile;
import com.sajitar.backend.domain.model.token.AttemptScope;
import com.sajitar.backend.domain.port.PasswordHasher;
import com.sajitar.backend.domain.port.checker.CheckerRepository;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.port.token.AttemptLimiter;
import com.sajitar.backend.domain.validation.profile.Birthday;
import com.sajitar.backend.domain.validation.profile.Description;
import com.sajitar.backend.domain.validation.profile.MasterRequiresTwoFactor;
import com.sajitar.backend.domain.validation.profile.Name;
import com.sajitar.backend.domain.validation.profile.Password;
import com.sajitar.backend.domain.validation.profile.TwoFactor;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PatchProfileUseCase {

    private final ProfileRepository profiles;

    private final CheckerRepository checkers;

    private final PasswordHasher passwordHasher;

    private final AttemptLimiter attempts;

    private final Validator validator;

    public Profile execute(final PatchProfileCommand command, final UUID viewerProfileId) {
        Constraints.requireValid(validator, command);
        validatePresentFields(command, validator);
        final var existing = profiles.findById(command.id()).orElseThrow(ProfileNotFoundException::new);
        final var type = command.type() == null ? existing.type() : command.type();
        final var name = command.name().orElse(existing.name());
        final var description = command.description().orElse(existing.description());
        final var birthday = command.birthday().orElse(existing.birthday());
        final var twoFactor = command.twoFactor().orElse(existing.twoFactor());
        requireOwnerWhenAttributesChange(viewerProfileId, existing, name, description, birthday, twoFactor);
        final var viewer = requireMasterWhenTypeChanges(viewerProfileId, existing.type(), type);
        MasterRequiresTwoFactor.Validation.validate(type, twoFactor);
        requireCallerPasswordWhenMasterBoundary(viewer, existing.type(), type, command.password(), command.address());
        final var saved = profiles.save(new Profile(
                existing.id(),
                type,
                name,
                description,
                birthday,
                existing.email(),
                existing.password(),
                twoFactor));
        discardSignInWhenNotRequired(saved);
        return saved;
    }

    private static void requireOwnerWhenAttributesChange(
            final UUID viewerProfileId,
            final Profile existing,
            final String name,
            final String description,
            final LocalDate birthday,
            final boolean twoFactor) {
        if (Objects.equals(name, existing.name())
                && Objects.equals(description, existing.description())
                && Objects.equals(birthday, existing.birthday())
                && twoFactor == existing.twoFactor()) {
            return;
        }
        if (existing.id().equals(viewerProfileId)) {
            return;
        }
        throw new ForbiddenProfileUpdateException();
    }

    private Profile requireMasterWhenTypeChanges(
            final UUID viewerProfileId,
            final Profile.Type current,
            final Profile.Type requested) {
        if (requested == current) {
            return null;
        }
        if (viewerProfileId != null) {
            final var viewer = profiles.findById(viewerProfileId).orElse(null);
            if (viewer != null && viewer.type().includes(Profile.Type.MASTER)) {
                return viewer;
            }
        }
        throw new ForbiddenProfileTypeException();
    }

    private void requireCallerPasswordWhenMasterBoundary(
            final Profile viewer,
            final Profile.Type current,
            final Profile.Type requested,
            final String password,
            final String address) {
        if (!crossesMasterBoundary(current, requested)) {
            return;
        }
        Password.Validation.validate(validator, password);
        requireCredentials(address, viewer.email());
        if (!passwordHasher.matches(password, viewer.password())) {
            throw new InvalidCredentialsException();
        }
    }

    private static boolean crossesMasterBoundary(final Profile.Type current, final Profile.Type requested) {
        return (current == Profile.Type.MASTER) != (requested == Profile.Type.MASTER);
    }

    private void requireCredentials(final String address, final String email) {
        Stream.of(
                attempts.register(AttemptScope.CREDENTIALS, address),
                attempts.register(AttemptScope.CREDENTIALS, email))
                .flatMap(Optional::stream)
                .max(Comparator.naturalOrder())
                .ifPresent(PatchProfileUseCase::tooMany);
    }

    private static void tooMany(final Duration retryAfter) {
        throw TooManyAttemptsException.forCredentials(retryAfter);
    }

    private static void validatePresentFields(final PatchProfileCommand command, final Validator validator) {
        if (command.name().isPresent()) {
            Name.Validation.validate(validator, command.name().orElse(null));
        }
        if (command.description().isPresent()) {
            Description.Validation.validate(validator, command.description().orElse(null));
        }
        if (command.birthday().isPresent()) {
            Birthday.Validation.validate(validator, command.birthday().orElse(null));
        }
        if (command.twoFactor().isPresent()) {
            TwoFactor.Validation.validate(validator, command.twoFactor().orElse(null));
        }
    }

    private void discardSignInWhenNotRequired(final Profile saved) {
        if (saved.requiresTwoFactor()) {
            return;
        }
        checkers.findByProfileIdAndType(saved.id(), Checker.Type.SIGN_IN)
                .ifPresent(checker -> checkers.deleteById(checker.id()));
    }

}
