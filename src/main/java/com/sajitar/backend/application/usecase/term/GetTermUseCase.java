package com.sajitar.backend.application.usecase.term;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sajitar.backend.domain.exception.ForbiddenTermException;
import com.sajitar.backend.domain.exception.TermNotFoundException;
import com.sajitar.backend.domain.exception.TermTypeAbsentException;
import com.sajitar.backend.domain.model.profile.Profile;
import com.sajitar.backend.domain.model.term.TermDetail;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.port.term.TermRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetTermUseCase {

    private final TermRepository terms;

    private final ProfileRepository profiles;

    public List<TermDetail> execute(final UUID id, final TermDetail.Type lang, final UUID viewerProfileId) {
        if (!isMaster(viewerProfileId)) {
            throw new ForbiddenTermException();
        }
        final var lines = terms.findByIdAll(id);
        if (lines.isEmpty()) {
            throw new TermNotFoundException();
        }
        if (lang == null) {
            return lines;
        }
        return lines.stream()
                .filter(line -> line.type() == lang)
                .findFirst()
                .map(List::of)
                .orElseThrow(TermTypeAbsentException::new);
    }

    private boolean isMaster(final UUID viewerProfileId) {
        return profiles.findById(viewerProfileId)
                .map(viewer -> viewer.type().includes(Profile.Type.MASTER))
                .orElse(false);
    }

}
