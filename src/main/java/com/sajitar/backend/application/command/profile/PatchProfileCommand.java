package com.sajitar.backend.application.command.profile;

import java.time.LocalDate;
import java.util.UUID;

import com.sajitar.backend.application.command.PatchValue;

import jakarta.validation.constraints.NotNull;

public record PatchProfileCommand(
        @NotNull UUID id,
        PatchValue<String> name,
        PatchValue<String> description,
        PatchValue<LocalDate> birthday,
        PatchValue<String> email,
        PatchValue<String> password) {

    public PatchProfileCommand {
        name = name == null ? PatchValue.absent() : name;
        description = description == null ? PatchValue.absent() : description;
        birthday = birthday == null ? PatchValue.absent() : birthday;
        email = email == null ? PatchValue.absent() : email;
        password = password == null ? PatchValue.absent() : password;
    }

    public boolean hasNewPassword() {
        final var raw = password.orElse(null);
        return password.isPresent() && raw != null && !raw.isBlank();
    }

}
