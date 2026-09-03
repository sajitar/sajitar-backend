package com.sajitar.backend.application.usecase;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.query.ListCheckersQuery;
import com.sajitar.backend.domain.model.Checker;
import com.sajitar.backend.domain.port.CheckerRepository;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListCheckersUseCase {

    private final CheckerRepository checkers;

    private final Validator validator;

    public List<Checker> execute(final ListCheckersQuery query) {
        Constraints.requireValid(validator, query);
        return checkers.findPage(query.toCriteria());
    }

}
