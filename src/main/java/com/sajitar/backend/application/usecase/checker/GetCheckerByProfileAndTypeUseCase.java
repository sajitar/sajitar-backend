package com.sajitar.backend.application.usecase.checker;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.query.checker.GetCheckerByProfileAndTypeQuery;
import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.port.checker.CheckerRepository;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetCheckerByProfileAndTypeUseCase {

    private final CheckerRepository checkers;

    private final Validator validator;

    public Optional<Checker> execute(final GetCheckerByProfileAndTypeQuery query) {
        Constraints.requireValid(validator, query);
        return checkers.findByProfileIdAndType(query.profileId(), query.type());
    }

}
