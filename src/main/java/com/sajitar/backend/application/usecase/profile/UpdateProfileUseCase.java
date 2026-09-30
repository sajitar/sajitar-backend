package com.sajitar.backend.application.usecase.profile;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.command.profile.UpdateProfileCommand;
import com.sajitar.backend.domain.exception.ForbiddenProfileTypeException;
import com.sajitar.backend.domain.exception.ForbiddenProfileUpdateException;
import com.sajitar.backend.domain.exception.ProfileNotFoundException;
import com.sajitar.backend.domain.model.profile.Profile;
import com.sajitar.backend.domain.port.profile.ProfileRepository;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdateProfileUseCase {

    private final ProfileRepository profiles;

    private final Validator validator;

    public Profile execute(final UpdateProfileCommand command, final UUID viewerProfileId) {
        Constraints.requireValid(validator, command);
        final var existing = profiles.findById(command.id()).orElseThrow(ProfileNotFoundException::new);
        requireOwnerWhenAttributesChange(
                viewerProfileId, existing, command.name(), command.description(), command.birthday());
        requireMasterWhenTypeChanges(viewerProfileId, existing.type(), command.type());
        return profiles.save(new Profile(
                existing.id(),
                command.type(),
                command.name(),
                command.description(),
                command.birthday(),
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

}
