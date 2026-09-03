package com.sajitar.backend.application.usecase.checker;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.command.checker.UpdateCheckerCommand;
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
public class UpdateCheckerUseCase {

    private final CheckerRepository checkers;

    private final Validator validator;

    public Checker execute(final UpdateCheckerCommand command) {
        Constraints.requireValid(validator, command);
        validatePresentFields(command, validator);
        final var existing = checkers.findById(command.id()).orElseThrow(CheckerNotFoundException::new);
        final var code = command.code() == null ? Checker.newCode() : command.code();
        final var attempts = command.attempts() == null ? Checker.ATTEMPTS_MAX : command.attempts();
        final var replaces = command.replaces() == null ? Checker.REPLACES_MAX : command.replaces();
        return checkers.save(new Checker(
                existing.id(),
                existing.profileId(),
                existing.type(),
                code,
                command.payload(),
                attempts,
                replaces,
                Instant.now()));
    }

    private static void validatePresentFields(final UpdateCheckerCommand command, final Validator validator) {
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
