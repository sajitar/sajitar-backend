package com.sajitar.backend.application.command.profile;

import java.util.UUID;

import com.sajitar.backend.domain.validation.checker.Code;

import jakarta.validation.constraints.NotNull;

public record DeleteProfileCommand(
        @NotNull UUID id,
        @NotNull UUID callerProfileId,
        @Code String code,
        String address) {
}
