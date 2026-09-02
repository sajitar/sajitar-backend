package com.sajitar.backend.application.usecase;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.command.CreateProfileCommand;
import com.sajitar.backend.domain.exception.EmailAlreadyRegisteredException;
import com.sajitar.backend.domain.model.Profile;
import com.sajitar.backend.domain.port.PasswordHasher;
import com.sajitar.backend.domain.port.ProfileRepository;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateProfileUseCase {

    private final ProfileRepository profiles;

    private final PasswordHasher passwordHasher;

    private final Validator validator;

    public Profile execute(final CreateProfileCommand command) {
        Constraints.requireValid(validator, command);
        profiles.findByEmail(command.email()).ifPresent(_ -> {
            throw new EmailAlreadyRegisteredException();
        });
        return profiles.save(Profile.create(
                command.name(),
                command.description(),
                command.birthday(),
                command.email(),
                passwordHasher.hash(command.password())));
    }

}
