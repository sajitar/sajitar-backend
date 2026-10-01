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
import com.sajitar.backend.application.command.profile.UpdateProfileCommand;
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
import com.sajitar.backend.domain.validation.profile.Password;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdateProfileUseCase {

    private final ProfileRepository profiles;

    private final CheckerRepository checkers;

    private final PasswordHasher passwordHasher;

    private final AttemptLimiter attempts;

    private final Validator validator;

    public Profile execute(final UpdateProfileCommand command, final UUID viewerProfileId) {
        Constraints.requireValid(validator, command);
        final var existing = profiles.findById(command.id()).orElseThrow(ProfileNotFoundException::new);
        requireOwnerWhenAttributesChange(
                viewerProfileId,
                existing,
                command.name(),
                command.description(),
                command.birthday(),
                command.twoFactor());
        final var viewer = requireMasterWhenTypeChanges(viewerProfileId, existing.type(), command.type());
        requireCallerPasswordWhenMasterBoundary(
                viewer,
                existing.type(),
                command.type(),
                command.password(),
                command.address());
        final var saved = profiles.save(new Profile(
                existing.id(),
                command.type(),
                command.name(),
                command.description(),
                command.birthday(),
                existing.email(),
                existing.password(),
                command.twoFactor()));
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
                .ifPresent(UpdateProfileUseCase::tooMany);
    }

    private static void tooMany(final Duration retryAfter) {
        throw TooManyAttemptsException.forCredentials(retryAfter);
    }

    private void discardSignInWhenNotRequired(final Profile saved) {
        if (saved.requiresTwoFactor()) {
            return;
        }
        checkers.findByProfileIdAndType(saved.id(), Checker.Type.SIGN_IN)
                .ifPresent(checker -> checkers.deleteById(checker.id()));
    }

}
