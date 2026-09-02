package com.sajitar.backend.application.usecase;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.command.PatchProfileCommand;
import com.sajitar.backend.domain.exception.EmailAlreadyRegisteredException;
import com.sajitar.backend.domain.exception.ProfileNotFoundException;
import com.sajitar.backend.domain.model.Profile;
import com.sajitar.backend.domain.port.PasswordHasher;
import com.sajitar.backend.domain.port.ProfileRepository;
import com.sajitar.backend.domain.validation.profile.Birthday;
import com.sajitar.backend.domain.validation.profile.Description;
import com.sajitar.backend.domain.validation.profile.Email;
import com.sajitar.backend.domain.validation.profile.Name;
import com.sajitar.backend.domain.validation.profile.Password;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PatchProfileUseCase {

    private final ProfileRepository profiles;

    private final PasswordHasher passwordHasher;

    private final Validator validator;

    public Profile execute(final PatchProfileCommand command) {
        Constraints.requireValid(validator, command);
        validatePresentFields(command, validator);
        final var existing = profiles.findById(command.id()).orElseThrow(ProfileNotFoundException::new);
        if (command.email().isPresent()) {
            profiles.findByEmail(command.email().orElse(null)).ifPresent(found -> {
                if (!found.id().equals(existing.id())) {
                    throw new EmailAlreadyRegisteredException();
                }
            });
        }
        final var password = command.hasNewPassword()
                ? passwordHasher.hash(command.password().orElse(null))
                : existing.password();
        return profiles.save(new Profile(
                existing.id(),
                command.name().orElse(existing.name()),
                command.description().orElse(existing.description()),
                command.birthday().orElse(existing.birthday()),
                command.email().orElse(existing.email()),
                password));
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
        if (command.email().isPresent()) {
            Email.Validation.validate(validator, command.email().orElse(null));
        }
        if (command.hasNewPassword()) {
            Password.Validation.validate(validator, command.password().orElse(null));
        }
    }

}
