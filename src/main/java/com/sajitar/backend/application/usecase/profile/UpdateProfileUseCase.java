package com.sajitar.backend.application.usecase.profile;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.command.profile.UpdateProfileCommand;
import com.sajitar.backend.domain.exception.EmailAlreadyRegisteredException;
import com.sajitar.backend.domain.exception.ProfileNotFoundException;
import com.sajitar.backend.domain.model.profile.Profile;
import com.sajitar.backend.domain.port.PasswordHasher;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.validation.profile.Password;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdateProfileUseCase {

    private final ProfileRepository profiles;

    private final PasswordHasher passwordHasher;

    private final Validator validator;

    public Profile execute(final UpdateProfileCommand command) {
        Constraints.requireValid(validator, command);
        if (command.hasNewPassword()) {
            Password.Validation.validate(validator, command.password());
        }
        final var existing = profiles.findById(command.id()).orElseThrow(ProfileNotFoundException::new);
        profiles.findByEmail(command.email()).ifPresent(found -> {
            if (!found.id().equals(existing.id())) {
                throw new EmailAlreadyRegisteredException();
            }
        });
        final var password = command.hasNewPassword()
                ? passwordHasher.hash(command.password())
                : existing.password();
        return profiles.save(new Profile(
                existing.id(),
                command.name(),
                command.description(),
                command.birthday(),
                command.email(),
                password));
    }

}
