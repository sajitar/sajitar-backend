package com.sajitar.backend.application.usecase.term;

import java.time.LocalDate;
import java.util.UUID;

import com.sajitar.backend.application.command.term.CreateTermCommand;
import com.sajitar.backend.application.command.term.CreateTermTypeCommand;
import com.sajitar.backend.domain.model.profile.Profile;
import com.sajitar.backend.domain.model.term.Term;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import lombok.experimental.UtilityClass;

@UtilityClass
final class TermUseCaseFixture {

    static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    static final UUID MASTER_ID = UUID.fromString("019c4000-0000-7000-8000-00000000000a");

    static final UUID READER_ID = UUID.fromString("019c4000-0000-7000-8000-00000000000b");

    static final UUID TERM_ID = UUID.fromString("019c4000-a111-7000-8000-111111111111");

    static final UUID OTHER_ID = UUID.fromString("019c4000-a222-7000-8000-222222222222");

    static CreateTermCommand validCreateCommand() {
        return new CreateTermCommand(Term.Type.EN, "Hello.");
    }

    static CreateTermTypeCommand validCreateTypeCommand() {
        return new CreateTermTypeCommand(TERM_ID, Term.Type.PT, "Olá.");
    }

    static Term draft(final UUID id, final Term.Type type) {
        return new Term(id, type, "Rascunho " + type, true);
    }

    static Term published(final UUID id, final Term.Type type) {
        return new Term(id, type, "Publicado " + type, false);
    }

    static Profile masterProfile() {
        return profile(MASTER_ID, Profile.Type.MASTER);
    }

    static Profile readerProfile() {
        return profile(READER_ID, Profile.Type.READER);
    }

    private static Profile profile(final UUID id, final Profile.Type type) {
        return new Profile(
                id,
                type,
                "Pessoa",
                "Uma pessoa.",
                LocalDate.parse("1988-01-10"),
                "person@example.com",
                "$2a$10$hashedPasswordHashValue012345678901",
                type.includes(Profile.Type.MASTER));
    }

}
