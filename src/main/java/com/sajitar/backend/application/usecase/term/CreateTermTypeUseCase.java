package com.sajitar.backend.application.usecase.term;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.command.term.CreateTermTypeCommand;
import com.sajitar.backend.domain.exception.DuplicateTermTypeException;
import com.sajitar.backend.domain.exception.ForbiddenTermException;
import com.sajitar.backend.domain.exception.TermNotFoundException;
import com.sajitar.backend.domain.model.profile.Profile;
import com.sajitar.backend.domain.model.term.Term;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.port.term.TermRepository;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateTermTypeUseCase {

    private final TermRepository terms;

    private final ProfileRepository profiles;

    private final Validator validator;

    public Term execute(final CreateTermTypeCommand command, final UUID viewerProfileId) {
        Constraints.requireValid(validator, command);
        if (!isMaster(viewerProfileId)) {
            throw new ForbiddenTermException();
        }
        final var lines = terms.findByIdAll(command.id());
        if (lines.isEmpty()) {
            throw new TermNotFoundException();
        }
        if (lines.stream().anyMatch(line -> line.type() == command.type())) {
            throw new DuplicateTermTypeException();
        }
        return terms.save(new Term(command.id(), command.type(), command.content(), true));
    }

    private boolean isMaster(final UUID viewerProfileId) {
        return profiles.findById(viewerProfileId)
                .map(viewer -> viewer.type().includes(Profile.Type.MASTER))
                .orElse(false);
    }

}
