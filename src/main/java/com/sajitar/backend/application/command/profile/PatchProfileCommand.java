package com.sajitar.backend.application.command.profile;

import java.time.LocalDate;
import java.util.UUID;

import com.sajitar.backend.application.command.PatchValue;
import com.sajitar.backend.domain.model.profile.Profile;

import jakarta.validation.constraints.NotNull;

public record PatchProfileCommand(
        @NotNull UUID id,
        Profile.Type type,
        PatchValue<String> name,
        PatchValue<String> description,
        PatchValue<LocalDate> birthday,
        PatchValue<Boolean> twoFactor,
        String password,
        String address) {

    public PatchProfileCommand {
        name = name == null ? PatchValue.absent() : name;
        description = description == null ? PatchValue.absent() : description;
        birthday = birthday == null ? PatchValue.absent() : birthday;
        twoFactor = twoFactor == null ? PatchValue.absent() : twoFactor;
    }

    public PatchProfileCommand(
            final UUID id,
            final Profile.Type type,
            final PatchValue<String> name,
            final PatchValue<String> description,
            final PatchValue<LocalDate> birthday,
            final PatchValue<Boolean> twoFactor) {
        this(id, type, name, description, birthday, twoFactor, null, null);
    }

}
