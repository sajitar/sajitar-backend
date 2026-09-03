package com.sajitar.backend.application.usecase.authority;

import java.time.LocalDate;
import java.util.UUID;

import com.sajitar.backend.application.command.authority.CreateAuthorityCommand;
import com.sajitar.backend.application.command.authority.PatchAuthorityCommand;
import com.sajitar.backend.application.command.authority.UpdateAuthorityCommand;
import com.sajitar.backend.domain.model.authority.Authority;
import com.sajitar.backend.domain.model.profile.Profile;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

final class AuthorityUseCaseFixture {

    static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    static final UUID ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    static final UUID PROFILE_ID = UUID.fromString("01989bad-6161-7000-0ae9-f440b10578ec");

    private AuthorityUseCaseFixture() {
    }

    static CreateAuthorityCommand validCreateCommand() {
        return new CreateAuthorityCommand(PROFILE_ID, Authority.Type.MASTER);
    }

    static UpdateAuthorityCommand identicalUpdateCommand() {
        return new UpdateAuthorityCommand(ID, Authority.Type.MASTER);
    }

    static PatchAuthorityCommand emptyPatchCommand() {
        return new PatchAuthorityCommand(ID, null);
    }

    static Authority persistedMaster() {
        return new Authority(ID, PROFILE_ID, Authority.Type.MASTER);
    }

    static Authority persistedMember() {
        return new Authority(ID, PROFILE_ID, Authority.Type.MEMBER);
    }

    static Authority persistedReader() {
        return new Authority(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440001"),
                PROFILE_ID,
                Authority.Type.READER);
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
