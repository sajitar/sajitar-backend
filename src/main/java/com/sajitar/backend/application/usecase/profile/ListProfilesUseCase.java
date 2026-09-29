package com.sajitar.backend.application.usecase.profile;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.page.Page;
import com.sajitar.backend.application.query.profile.ListProfilesQuery;
import com.sajitar.backend.domain.exception.ForbiddenProfileTypeException;
import com.sajitar.backend.domain.exception.ForbiddenProfileVerifiedException;
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
        final var master = profiles.findById(query.viewerProfileId())
                .map(viewer -> viewer.type().includes(Profile.Type.MASTER))
                .orElse(false);
        if (query.type() != null && !master) {
            throw new ForbiddenProfileTypeException();
        }
        if (query.verified() != null && !master) {
            throw new ForbiddenProfileVerifiedException();
        }
        final var criteria = query.toCriteria(master);
        final var content = profiles.findPage(criteria);
        if (content.isEmpty()) {
            return Page.empty(query.reverse());
        }
        final var last = content.getLast();
        final long following = profiles.countAfterCursor(
                criteria.withCursor(last.name(), last.id(), query.reverse()));
        final long preceding = query.hasCursor()
                ? profiles.countAfterCursor(
                        criteria.withCursor(content.getFirst().name(), content.getFirst().id(), !query.reverse()))
                : 0L;
        return new Page<>(content, preceding, following, query.reverse());
    }

}
