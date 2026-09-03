package com.sajitar.backend.application.usecase;

import java.time.Instant;
import java.util.UUID;

import com.sajitar.backend.application.command.CreateCheckerCommand;
import com.sajitar.backend.application.command.PatchCheckerCommand;
import com.sajitar.backend.application.command.UpdateCheckerCommand;
import com.sajitar.backend.domain.model.Checker;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

final class CheckerUseCaseFixture {

    static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    static final UUID ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    static final UUID PROFILE_ID = UUID.fromString("01989bad-6161-7000-0ae9-f440b10578ec");

    static final Instant UPDATED_AT = Instant.parse("2001-04-24T21:00:00Z");

    private CheckerUseCaseFixture() {
    }

    static CreateCheckerCommand validCreateCommand() {
        return new CreateCheckerCommand(PROFILE_ID, Checker.Type.CHANGE_EMAIL);
    }

    static UpdateCheckerCommand emptyUpdateCommand() {
        return new UpdateCheckerCommand(ID, null, null, null, null);
    }

    static PatchCheckerCommand emptyPatchCommand() {
        return new PatchCheckerCommand(ID, null, null, null, null);
    }

    static Checker persistedChecker() {
        return new Checker(ID, PROFILE_ID, Checker.Type.CHANGE_EMAIL, "123456", null, 10, 3, UPDATED_AT);
    }

    static Checker persistedVerifyEmail() {
        return new Checker(ID, PROFILE_ID, Checker.Type.VERIFY_EMAIL, "123456", null, 10, 3, UPDATED_AT);
    }

}
