package com.sajitar.backend.application.usecase.profile;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.command.profile.PatchProfileCommand;
import com.sajitar.backend.domain.exception.ForbiddenProfileTypeException;
import com.sajitar.backend.domain.exception.ForbiddenProfileUpdateException;
import com.sajitar.backend.domain.exception.ProfileNotFoundException;
import com.sajitar.backend.domain.model.profile.Profile;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.validation.profile.Birthday;
import com.sajitar.backend.domain.validation.profile.Description;
import com.sajitar.backend.domain.validation.profile.Name;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PatchProfileUseCase {

    private final ProfileRepository profiles;

    private final Validator validator;

    public Profile execute(final PatchProfileCommand command, final UUID viewerProfileId) {
        Constraints.requireValid(validator, command);
        validatePresentFields(command, validator);
        final var existing = profiles.findById(command.id()).orElseThrow(ProfileNotFoundException::new);
        final var type = command.type() == null ? existing.type() : command.type();
        final var name = command.name().orElse(existing.name());
        final var description = command.description().orElse(existing.description());
        final var birthday = command.birthday().orElse(existing.birthday());
        requireOwnerWhenAttributesChange(viewerProfileId, existing, name, description, birthday);
        requireMasterWhenTypeChanges(viewerProfileId, existing.type(), type);
        return profiles.save(new Profile(
                existing.id(),
                type,
                name,
                description,
                birthday,
                existing.email(),
                existing.password()));
    }

    private static void requireOwnerWhenAttributesChange(
            final UUID viewerProfileId,
            final Profile existing,
            final String name,
            final String description,
            final LocalDate birthday) {
        if (Objects.equals(name, existing.name())
                && Objects.equals(description, existing.description())
                && Objects.equals(birthday, existing.birthday())) {
            return;
        }
        if (existing.id().equals(viewerProfileId)) {
            return;
        }
        throw new ForbiddenProfileUpdateException();
    }

    private void requireMasterWhenTypeChanges(
            final UUID viewerProfileId,
            final Profile.Type current,
            final Profile.Type requested) {
        if (requested == current) {
            return;
        }
        if (viewerProfileId != null && profiles.findById(viewerProfileId)
                .map(viewer -> viewer.type().includes(Profile.Type.MASTER))
                .orElse(false)) {
            return;
        }
        throw new ForbiddenProfileTypeException();
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
    }

}
