package com.sajitar.backend.application.usecase.note;

import java.util.Objects;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.command.note.UpdateNoteCommand;
import com.sajitar.backend.domain.exception.NoteNotFoundException;
import com.sajitar.backend.domain.model.note.Note;
import com.sajitar.backend.domain.port.note.NoteRepository;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdateNoteUseCase {

    private final NoteRepository notes;

    private final Validator validator;

    public Note execute(final UpdateNoteCommand command) {
        Constraints.requireValid(validator, command);
        final var existing = notes.findById(command.id()).orElseThrow(NoteNotFoundException::new);
        return persistIfChanged(existing, command.type(), command.content());
    }

    private Note persistIfChanged(final Note existing, final Note.Type nextType, final String nextContent) {
        if (existing.type() == nextType && Objects.equals(existing.content(), nextContent)) {
            return existing;
        }
        return notes.save(existing.withType(nextType).withContent(nextContent));
    }

}
