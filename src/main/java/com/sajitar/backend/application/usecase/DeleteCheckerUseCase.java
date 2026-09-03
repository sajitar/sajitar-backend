package com.sajitar.backend.application.usecase;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.command.DeleteCheckerCommand;
import com.sajitar.backend.domain.exception.CheckerNotFoundException;
import com.sajitar.backend.domain.exception.CheckerTypeRestrictedException;
import com.sajitar.backend.domain.port.CheckerRepository;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeleteCheckerUseCase {

    private final CheckerRepository checkers;

    private final Validator validator;

    public void execute(final DeleteCheckerCommand command) {
        Constraints.requireValid(validator, command);
        final var existing = checkers.findById(command.id()).orElseThrow(CheckerNotFoundException::new);
        if (existing.type().restrict()) {
            throw CheckerTypeRestrictedException.forDelete();
        }
        checkers.deleteById(command.id());
    }

}
