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
        PatchValue<String> email) {

    public PatchProfileCommand {
        name = name == null ? PatchValue.absent() : name;
        description = description == null ? PatchValue.absent() : description;
        birthday = birthday == null ? PatchValue.absent() : birthday;
        email = email == null ? PatchValue.absent() : email;
    }

}
