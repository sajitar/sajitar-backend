package com.sajitar.backend.application.command.profile;

import java.util.UUID;

import com.sajitar.backend.domain.validation.profile.DifferentPasswords;
import com.sajitar.backend.domain.validation.profile.Password;

import jakarta.validation.constraints.NotNull;

@DifferentPasswords
public record ChangeOwnPasswordCommand(
        @NotNull UUID profileId,
        @Password String currentPassword,
        @Password String newPassword,
        boolean wipe,
        String address) implements DifferentPasswords.Pair {

}
