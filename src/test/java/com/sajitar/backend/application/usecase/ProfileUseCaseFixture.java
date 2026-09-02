package com.sajitar.backend.application.usecase;

import java.time.LocalDate;
import java.util.UUID;

import com.sajitar.backend.application.command.CreateProfileCommand;
import com.sajitar.backend.application.command.PatchProfileCommand;
import com.sajitar.backend.application.command.UpdateProfileCommand;
import com.sajitar.backend.domain.model.Profile;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

final class ProfileUseCaseFixture {

    static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    static final UUID ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    static final String NAME = "Maria Silva";

    static final String DESCRIPTION = "Uma pessoa criativa e dedicada.";

    static final LocalDate BIRTHDAY = LocalDate.parse("1988-01-10");

    static final String EMAIL = "user@example.com";

    static final String PASSWORD = "12345678";

    private ProfileUseCaseFixture() {
    }

    static CreateProfileCommand validCreateCommand() {
        return new CreateProfileCommand(NAME, DESCRIPTION, BIRTHDAY, EMAIL, PASSWORD);
    }

    static UpdateProfileCommand validUpdateCommand() {
        return new UpdateProfileCommand(ID, NAME, DESCRIPTION, BIRTHDAY, EMAIL, null);
    }

    static PatchProfileCommand emptyPatchCommand() {
        return new PatchProfileCommand(ID, null, null, null, null, null);
    }

    static Profile persistedProfile() {
        return new Profile(ID, NAME, DESCRIPTION, BIRTHDAY, EMAIL, "$2a$10$hashedPasswordHashValue012345678901");
    }

}
