package com.sajitar.backend.application.usecase.note;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.command.note.CreateNoteCommand;
import com.sajitar.backend.domain.exception.ProfileUnavailableException;
import com.sajitar.backend.domain.model.note.Note;
import com.sajitar.backend.domain.port.note.NoteRepository;
import com.sajitar.backend.domain.port.profile.ProfileRepository;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateNoteUseCase {

    private final NoteRepository notes;

    private final ProfileRepository profiles;

    private final Validator validator;

    public Note execute(final CreateNoteCommand command) {
        Constraints.requireValid(validator, command);
        profiles.findById(command.profileId()).orElseThrow(ProfileUnavailableException::new);
        return notes.save(Note.create(command.profileId(), command.type(), command.content()));
    }

}
