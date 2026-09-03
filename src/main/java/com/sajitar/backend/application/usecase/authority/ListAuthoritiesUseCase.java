package com.sajitar.backend.application.usecase.authority;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.page.Page;
import com.sajitar.backend.application.query.authority.ListAuthoritiesQuery;
import com.sajitar.backend.domain.model.authority.Authority;
import com.sajitar.backend.domain.port.authority.AuthorityRepository;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListAuthoritiesUseCase {

    private final AuthorityRepository authorities;

    private final Validator validator;

    public Page<Authority> execute(final ListAuthoritiesQuery query) {
        Constraints.requireValid(validator, query);
        final var content = authorities.findPage(query.toCriteria());
        if (content.isEmpty()) {
            return Page.empty(query.reverse());
        }
        final var last = content.getLast();
        final long following = authorities.countAfterCursor(
                query.toCriteria().withCursor(last.type(), query.reverse()));
        final long preceding = query.hasCursor()
                ? authorities.countAfterCursor(
                        query.toCriteria().withCursor(content.getFirst().type(), !query.reverse()))
                : 0L;
        return new Page<>(content, preceding, following, query.reverse());
    }

}
