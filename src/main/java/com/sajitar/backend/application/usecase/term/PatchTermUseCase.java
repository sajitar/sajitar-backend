package com.sajitar.backend.application.usecase.term;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.command.PatchValue;
import com.sajitar.backend.application.command.term.PatchTermCommand;
import com.sajitar.backend.domain.exception.ForbiddenTermException;
import com.sajitar.backend.domain.exception.InvalidTermDraftException;
import com.sajitar.backend.domain.exception.TermNotFoundException;
import com.sajitar.backend.domain.exception.TermTypeAbsentException;
import com.sajitar.backend.domain.model.profile.Profile;
import com.sajitar.backend.domain.model.term.Term;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.port.term.TermRepository;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PatchTermUseCase {

    private final TermRepository terms;

    private final ProfileRepository profiles;

    private final Validator validator;

    public Term execute(final PatchTermCommand command, final UUID viewerProfileId) {
        Constraints.requireValid(validator, command);
        if (!isMaster(viewerProfileId)) {
            throw new ForbiddenTermException();
        }
        final var lines = terms.findByIdAll(command.id());
        if (lines.isEmpty()) {
            throw new TermNotFoundException();
        }
        final var existing = lines.stream()
                .filter(line -> line.type() == command.type())
                .findFirst()
                .orElseThrow(TermTypeAbsentException::new);
        if (!existing.draft()) {
            throw InvalidTermDraftException.closed();
        }
        final var nextDraft = resolveDraft(command.draft(), lines);
        return terms.save(existing.withContent(command.content()).withDraft(nextDraft));
    }

    private boolean resolveDraft(final PatchValue<Boolean> draft, final List<Term> lines) {
        if (!draft.isPresent()) {
            return true;
        }
        final var value = draft.orElse(null);
        if (value == null) {
            throw InvalidTermDraftException.nullValue();
        }
        if (value) {
            return true;
        }
        if (!hasAllTypes(lines)) {
            throw InvalidTermDraftException.incomplete();
        }
        return false;
    }

    private boolean hasAllTypes(final List<Term> lines) {
        return lines.stream()
                .map(Term::type)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Term.Type.class)))
                .containsAll(EnumSet.allOf(Term.Type.class));
    }

    private boolean isMaster(final UUID viewerProfileId) {
        return profiles.findById(viewerProfileId)
                .map(viewer -> viewer.type().includes(Profile.Type.MASTER))
                .orElse(false);
    }

}
