package com.sajitar.backend.application.usecase.term;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.page.Page;
import com.sajitar.backend.application.query.term.ListTermsQuery;
import com.sajitar.backend.domain.exception.ForbiddenTermException;
import com.sajitar.backend.domain.model.profile.Profile;
import com.sajitar.backend.domain.model.term.TermDetail;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.port.term.TermRepository;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListTermsUseCase {

    private final TermRepository terms;

    private final ProfileRepository profiles;

    private final Validator validator;

    public Page<List<TermDetail>> execute(final ListTermsQuery query, final UUID viewerProfileId) {
        Constraints.requireValid(validator, query);
        if (!isMaster(viewerProfileId)) {
            throw new ForbiddenTermException();
        }
        final var ids = terms.findPageIds(query.toCriteria());
        if (ids.isEmpty()) {
            return Page.empty(query.reverse());
        }
        final var content = ids.stream().map(id -> linesFor(id, query.type())).toList();
        final long following = terms.countAfterCursor(
                query.toCriteria().withCursor(ids.getLast(), query.reverse()));
        final long preceding = query.hasCursor()
                ? terms.countAfterCursor(query.toCriteria().withCursor(ids.getFirst(), !query.reverse()))
                : 0L;
        return new Page<>(content, preceding, following, query.reverse());
    }

    private List<TermDetail> linesFor(final UUID id, final TermDetail.Type lang) {
        final var lines = terms.findByIdAll(id);
        return lang == null ? lines : lines.stream().filter(line -> line.type() == lang).toList();
    }

    private boolean isMaster(final UUID viewerProfileId) {
        return profiles.findById(viewerProfileId)
                .map(viewer -> viewer.type().includes(Profile.Type.MASTER))
                .orElse(false);
    }

}
