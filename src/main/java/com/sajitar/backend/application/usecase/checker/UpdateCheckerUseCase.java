package com.sajitar.backend.application.usecase.checker;

import java.util.Objects;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.command.checker.UpdateCheckerCommand;
import com.sajitar.backend.domain.exception.CheckerNotFoundException;
import com.sajitar.backend.domain.exception.CheckerTypeAlreadyExistsException;
import com.sajitar.backend.domain.exception.CheckerTypeRestrictedException;
import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.port.checker.CheckerRepository;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdateCheckerUseCase {

    private final CheckerRepository checkers;

    private final Validator validator;

    public Checker execute(final UpdateCheckerCommand command) {
        Constraints.requireValid(validator, command);
        final var existing = checkers.findById(command.id()).orElseThrow(CheckerNotFoundException::new);
        return persistIfChanged(existing, command.type(), command.payload());
    }

    private Checker persistIfChanged(final Checker existing, final Checker.Type nextType, final String nextPayload) {
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
