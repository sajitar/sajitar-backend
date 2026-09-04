package com.sajitar.backend.application.usecase.note;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.domain.model.note.Note;
import com.sajitar.backend.domain.port.note.NoteRepository;

import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetNoteUseCase {

    private final NoteRepository notes;

    private final Validator validator;

    public Optional<Note> execute(final UUID id) {
        Constraints.requireValid(validator, new IdQuery(id));
        return notes.findById(id);
    }

    private record IdQuery(@NotNull(message = "{validation.not-null}") UUID id) {
    }

}
