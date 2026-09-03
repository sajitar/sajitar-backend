package com.sajitar.backend.application.usecase.checker;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.command.checker.CreateCheckerCommand;
import com.sajitar.backend.domain.exception.CheckerTypeAlreadyExistsException;
import com.sajitar.backend.domain.exception.CheckerTypeRestrictedException;
import com.sajitar.backend.domain.exception.ProfileUnavailableException;
import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.port.checker.CheckerRepository;
import com.sajitar.backend.domain.port.profile.ProfileRepository;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateCheckerUseCase {

    private final CheckerRepository checkers;

    private final ProfileRepository profiles;

    private final Validator validator;

    public Checker execute(final CreateCheckerCommand command) {
        Constraints.requireValid(validator, command);
        if (command.type().restrict()) {
            throw CheckerTypeRestrictedException.forCreate();
        }
        profiles.findById(command.profileId()).orElseThrow(ProfileUnavailableException::new);
        checkers.findByProfileIdAndType(command.profileId(), command.type()).ifPresent(_ -> {
            throw new CheckerTypeAlreadyExistsException();
        });
        return checkers.save(Checker.create(command.profileId(), command.type()).withPayload(command.payload()));
    }

}
