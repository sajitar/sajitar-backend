package com.sajitar.backend.application.usecase.checker;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.command.checker.DeleteCheckerCommand;
import com.sajitar.backend.domain.exception.CheckerNotFoundException;
import com.sajitar.backend.domain.port.checker.CheckerRepository;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeleteCheckerUseCase {

    private final CheckerRepository checkers;

    private final Validator validator;

    public void execute(final DeleteCheckerCommand command) {
        Constraints.requireValid(validator, command);
        checkers.findById(command.id()).orElseThrow(CheckerNotFoundException::new);
        checkers.deleteById(command.id());
    }

}
