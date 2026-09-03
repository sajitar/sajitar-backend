package com.sajitar.backend.application.usecase.profile;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.page.Page;
import com.sajitar.backend.application.query.profile.ListProfilesQuery;
import com.sajitar.backend.domain.model.profile.Profile;
import com.sajitar.backend.domain.port.profile.ProfileRepository;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListProfilesUseCase {

    private final ProfileRepository profiles;

    private final Validator validator;

    public Page<Profile> execute(final ListProfilesQuery query) {
        Constraints.requireValid(validator, query);
        final var content = profiles.findPage(query.toCriteria());
        if (content.isEmpty()) {
            return Page.empty(query.reverse());
        }
        final var last = content.getLast();
        final long following = profiles.countAfterCursor(
                query.toCriteria().withCursor(last.name(), last.id(), query.reverse()));
        final long preceding = query.hasCursor()
                ? profiles.countAfterCursor(
                        query.toCriteria().withCursor(content.getFirst().name(), content.getFirst().id(), !query.reverse()))
                : 0L;
        return new Page<>(content, preceding, following, query.reverse());
    }

}
