package com.sajitar.backend.application.usecase.checker;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.page.Page;
import com.sajitar.backend.application.query.checker.ListCheckersQuery;
import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.port.checker.CheckerRepository;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListCheckersUseCase {

    private final CheckerRepository checkers;

    private final Validator validator;

    public Page<Checker> execute(final ListCheckersQuery query) {
        Constraints.requireValid(validator, query);
        final var content = checkers.findPage(query.toCriteria());
        if (content.isEmpty()) {
            return Page.empty(false);
        }
        final var last = content.getLast();
        final long following = checkers.countAfterCursor(
                query.toCriteria().withCursor(last.type(), false));
        final long preceding = query.hasCursor()
                ? checkers.countAfterCursor(
                        query.toCriteria().withCursor(content.getFirst().type(), true))
                : 0L;
        return new Page<>(content, preceding, following, false);
    }

}
