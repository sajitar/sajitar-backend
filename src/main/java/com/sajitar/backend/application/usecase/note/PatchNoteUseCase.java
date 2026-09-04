package com.sajitar.backend.application.usecase.note;

import java.util.Objects;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.command.note.PatchNoteCommand;
import com.sajitar.backend.domain.exception.NoteNotFoundException;
import com.sajitar.backend.domain.model.note.Note;
import com.sajitar.backend.domain.port.note.NoteRepository;
import com.sajitar.backend.domain.validation.note.Content;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PatchNoteUseCase {

    private final NoteRepository notes;

    private final Validator validator;

    public Note execute(final PatchNoteCommand command) {
        Constraints.requireValid(validator, command);
        if (command.content().isPresent()) {
            Content.Validation.validate(validator, command.content().orElse(null));
        }
        final var existing = notes.findById(command.id()).orElseThrow(NoteNotFoundException::new);
        if (!command.hasChanges()) {
            return existing;
        }
        final var nextType = command.type() != null ? command.type() : existing.type();
        final var nextContent = command.content().orElse(existing.content());
        if (existing.type() == nextType && Objects.equals(existing.content(), nextContent)) {
            return existing;
        }
        return notes.save(existing.withType(nextType).withContent(nextContent));
    }

}
