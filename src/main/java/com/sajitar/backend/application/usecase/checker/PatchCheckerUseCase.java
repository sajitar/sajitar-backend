package com.sajitar.backend.application.usecase.checker;

import java.util.Objects;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.command.checker.PatchCheckerCommand;
import com.sajitar.backend.domain.exception.CheckerNotFoundException;
import com.sajitar.backend.domain.exception.CheckerTypeAlreadyExistsException;
import com.sajitar.backend.domain.exception.CheckerTypeRestrictedException;
import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.port.checker.CheckerRepository;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PatchCheckerUseCase {

    private final CheckerRepository checkers;

    private final Validator validator;

    public Checker execute(final PatchCheckerCommand command) {
        Constraints.requireValid(validator, command);
        final var existing = checkers.findById(command.id()).orElseThrow(CheckerNotFoundException::new);
        if (!command.hasChanges()) {
            return existing;
        }
        final var nextType = command.type() != null ? command.type() : existing.type();
        final var nextPayload = command.payload().orElse(existing.payload());
        if (existing.type() == nextType && Objects.equals(existing.payload(), nextPayload)) {
            return existing;
        }
        if (nextType != existing.type()) {
            if (nextType.restrict()) {
                throw CheckerTypeRestrictedException.forCreate();
            }
            checkers.findByProfileIdAndType(existing.profileId(), nextType).ifPresent(_ -> {
                throw new CheckerTypeAlreadyExistsException();
            });
        }
        return checkers.save(existing.consumeReplace(nextType, nextPayload));
    }

}
