package com.sajitar.backend.application.usecase.note;

import java.time.LocalDate;
import java.util.UUID;

import com.sajitar.backend.application.command.note.CreateNoteCommand;
import com.sajitar.backend.application.command.note.PatchNoteCommand;
import com.sajitar.backend.application.command.note.UpdateNoteCommand;
import com.sajitar.backend.domain.model.note.Note;
import com.sajitar.backend.domain.model.profile.Profile;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

final class NoteUseCaseFixture {

    static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    static final UUID ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    static final UUID PROFILE_ID = UUID.fromString("01989bad-6161-7000-0ae9-f440b10578ec");

    static final String CONTENT = "Uma nota.";

    private NoteUseCaseFixture() {
    }

    static CreateNoteCommand validCreateCommand() {
        return new CreateNoteCommand(PROFILE_ID, Note.Type.PUBLIC, CONTENT);
    }

    static UpdateNoteCommand identicalUpdateCommand() {
        return new UpdateNoteCommand(ID, Note.Type.PUBLIC, CONTENT);
    }

    static PatchNoteCommand emptyPatchCommand() {
        return new PatchNoteCommand(ID, null, null);
    }

    static Note persistedPublic() {
        return new Note(ID, PROFILE_ID, Note.Type.PUBLIC, CONTENT);
    }

    static Note persistedProtected() {
        return new Note(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440001"),
                PROFILE_ID,
                Note.Type.PROTECTED,
                "Outra nota.");
    }

    static Profile availableProfile() {
        return new Profile(
                PROFILE_ID,
                "Maria Silva",
                "Uma pessoa criativa e dedicada.",
                LocalDate.parse("1988-01-10"),
                "user@example.com",
                "$2a$10$hashedPasswordHashValue012345678901");
    }

}
