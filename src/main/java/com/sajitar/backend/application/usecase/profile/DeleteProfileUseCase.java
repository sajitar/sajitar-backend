package com.sajitar.backend.application.usecase.profile;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.command.profile.DeleteProfileCommand;
import com.sajitar.backend.domain.exception.ProfileNotFoundException;
import com.sajitar.backend.domain.port.profile.ProfileRepository;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeleteProfileUseCase {

    private final ProfileRepository profiles;

    private final Validator validator;

    public void execute(final DeleteProfileCommand command) {
        Constraints.requireValid(validator, command);
        profiles.findById(command.id()).orElseThrow(ProfileNotFoundException::new);
        profiles.deleteById(command.id());
    }

}
