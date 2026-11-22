package com.sajitar.backend.application.usecase.term;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
import com.sajitar.backend.domain.model.term.TermDetail;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.port.term.TermRepository;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PatchTermUseCase {

    private final TermRepository terms;

    private final ProfileRepository profiles;

    private final Validator validator;

    public TermDetail execute(final PatchTermCommand command, final UUID viewerProfileId) {
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
        if (isClosed(lines)) {
            throw InvalidTermDraftException.closed();
        }
        validateBody(command);
        final var nextContent = command.content().orElse(existing.content());
        final var nextDraft = resolveDraft(command.draft(), existing, lines);
        return terms.save(new Term(existing.termId()), existing.withContent(nextContent).withDraft(nextDraft));
    }

    private boolean isClosed(final List<TermDetail> lines) {
        return hasAllTypes(lines) && lines.stream().noneMatch(TermDetail::draft);
    }

    private void validateBody(final PatchTermCommand command) {
        final var content = command.content();
        final var draft = command.draft();
        final Set<ConstraintViolation<?>> violations = new LinkedHashSet<>();
        if (!content.isPresent() && !draft.isPresent()) {
            violations.addAll(validator.validate(new PresentContent(null)));
            violations.addAll(validator.validate(new PresentDraft(null)));
        } else {
            if (content.isPresent()) {
                violations.addAll(validator.validate(new PresentContent(content.orElse(null))));
            }
            if (draft.isPresent()) {
                violations.addAll(validator.validate(new PresentDraft(draft.orElse(null))));
            }
        }
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private boolean resolveDraft(
            final PatchValue<Boolean> draft, final TermDetail existing, final List<TermDetail> lines) {
        if (!draft.isPresent()) {
            return existing.draft();
        }
        if (Boolean.TRUE.equals(draft.orElse(null))) {
            return true;
        }
        if (!hasAllTypes(lines)) {
            throw InvalidTermDraftException.incomplete();
        }
        return false;
    }

    private boolean hasAllTypes(final List<TermDetail> lines) {
        return lines.stream()
                .map(TermDetail::type)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(TermDetail.Type.class)))
                .containsAll(EnumSet.allOf(TermDetail.Type.class));
    }

    private boolean isMaster(final UUID viewerProfileId) {
        return profiles.findById(viewerProfileId)
                .map(viewer -> viewer.type().includes(Profile.Type.MASTER))
                .orElse(false);
    }

    private static final class PresentContent {

        @NotBlank(message = "{validation.not-blank}")
        private final String content;

        private PresentContent(final String content) {
            this.content = content;
        }

    }

    private static final class PresentDraft {

        @NotNull(message = "{validation.not-null}")
        private final Boolean draft;

        private PresentDraft(final Boolean draft) {
            this.draft = draft;
        }

    }

}
