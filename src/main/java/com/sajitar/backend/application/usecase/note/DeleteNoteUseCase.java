package com.sajitar.backend.application.usecase.note;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.command.note.DeleteNoteCommand;
import com.sajitar.backend.domain.exception.NoteNotFoundException;
import com.sajitar.backend.domain.port.note.NoteRepository;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeleteNoteUseCase {

    private final NoteRepository notes;

    private final Validator validator;

    public void execute(final DeleteNoteCommand command) {
        Constraints.requireValid(validator, command);
        notes.findById(command.id()).orElseThrow(NoteNotFoundException::new);
        notes.deleteById(command.id());
    }

}
