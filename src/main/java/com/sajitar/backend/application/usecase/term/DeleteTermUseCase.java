package com.sajitar.backend.application.usecase.term;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sajitar.backend.domain.exception.ForbiddenTermException;
import com.sajitar.backend.domain.exception.TermNotFoundException;
import com.sajitar.backend.domain.model.profile.Profile;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.port.term.TermRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeleteTermUseCase {

    private final TermRepository terms;

    private final ProfileRepository profiles;

    public void execute(final UUID id, final UUID viewerProfileId) {
        if (!isMaster(viewerProfileId)) {
            throw new ForbiddenTermException();
        }
        if (terms.findByIdAll(id).isEmpty()) {
            throw new TermNotFoundException();
        }
        terms.deleteById(id);
    }

    private boolean isMaster(final UUID viewerProfileId) {
        return profiles.findById(viewerProfileId)
                .map(viewer -> viewer.type().includes(Profile.Type.MASTER))
                .orElse(false);
    }

}
