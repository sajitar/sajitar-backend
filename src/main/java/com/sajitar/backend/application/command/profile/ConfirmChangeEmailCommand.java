package com.sajitar.backend.application.command.profile;

import java.util.UUID;

import com.sajitar.backend.domain.validation.checker.Code;
import com.sajitar.backend.domain.validation.profile.Email;

import jakarta.validation.constraints.NotNull;

public record ConfirmChangeEmailCommand(
        @NotNull UUID profileId,
        @Code String code,
        @Email String newEmail,
        String address) {
}
