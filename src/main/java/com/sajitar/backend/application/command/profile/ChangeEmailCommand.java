package com.sajitar.backend.application.command.profile;

import java.util.UUID;

import com.sajitar.backend.domain.validation.checker.Code;

import jakarta.validation.constraints.NotNull;

public record ChangeEmailCommand(
        @NotNull UUID profileId,
        @Code String code,
        String address) {
}
