package com.sajitar.backend.application.usecase;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.domain.model.Checker;
import com.sajitar.backend.domain.port.CheckerRepository;

import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetCheckerUseCase {

    private final CheckerRepository checkers;

    private final Validator validator;

    public Optional<Checker> execute(final UUID id) {
        Constraints.requireValid(validator, new IdQuery(id));
        return checkers.findById(id);
    }

    private record IdQuery(@NotNull(message = "{validation.not-null}") UUID id) {
    }

}
