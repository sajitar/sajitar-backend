package com.sajitar.backend.application.usecase.checker;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.command.checker.PatchCheckerCommand;
import com.sajitar.backend.domain.exception.CheckerNotFoundException;
import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.port.checker.CheckerRepository;
import com.sajitar.backend.domain.validation.checker.CheckerAttempts;
import com.sajitar.backend.domain.validation.checker.CheckerCode;
import com.sajitar.backend.domain.validation.checker.CheckerReplaces;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PatchCheckerUseCase {

    private final CheckerRepository checkers;

    private final Validator validator;

    public Checker execute(final PatchCheckerCommand command) {
        Constraints.requireValid(validator, command);
        validatePresentFields(command, validator);
        final var existing = checkers.findById(command.id()).orElseThrow(CheckerNotFoundException::new);
        if (!command.hasChanges()) {
            return existing;
        }
        return checkers.save(new Checker(
                existing.id(),
                existing.profileId(),
                existing.type(),
                command.code() != null ? command.code() : existing.code(),
                command.payload() != null ? command.payload() : existing.payload(),
                command.attempts() != null ? command.attempts() : existing.attempts(),
                command.replaces() != null ? command.replaces() : existing.replaces(),
                Instant.now()));
    }

    private static void validatePresentFields(final PatchCheckerCommand command, final Validator validator) {
        if (command.code() != null) {
            CheckerCode.Validation.validate(validator, command.code());
        }
        if (command.attempts() != null) {
            CheckerAttempts.Validation.validate(validator, command.attempts());
        }
        if (command.replaces() != null) {
            CheckerReplaces.Validation.validate(validator, command.replaces());
        }
    }

}
