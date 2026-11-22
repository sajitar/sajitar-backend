package com.sajitar.backend.application.usecase.term;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.command.term.CreateTermCommand;
import com.sajitar.backend.domain.exception.ForbiddenTermException;
import com.sajitar.backend.domain.model.profile.Profile;
import com.sajitar.backend.domain.model.term.Term;
import com.sajitar.backend.domain.model.term.TermDetail;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.port.term.TermRepository;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateTermUseCase {

    private final TermRepository terms;

    private final ProfileRepository profiles;

    private final Validator validator;

    public TermDetail execute(final CreateTermCommand command, final UUID viewerProfileId) {
        Constraints.requireValid(validator, command);
        if (!isMaster(viewerProfileId)) {
            throw new ForbiddenTermException();
        }
        final var term = Term.create();
        return terms.save(term, TermDetail.create(term.id(), command.type(), command.content()));
    }

    private boolean isMaster(final UUID viewerProfileId) {
        return profiles.findById(viewerProfileId)
                .map(viewer -> viewer.type().includes(Profile.Type.MASTER))
                .orElse(false);
    }

}
