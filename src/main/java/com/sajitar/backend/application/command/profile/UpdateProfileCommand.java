package com.sajitar.backend.application.command.profile;

import java.time.LocalDate;
import java.util.UUID;

import com.sajitar.backend.domain.model.profile.Profile;
import com.sajitar.backend.domain.validation.profile.Birthday;
import com.sajitar.backend.domain.validation.profile.Description;
import com.sajitar.backend.domain.validation.profile.MasterRequiresTwoFactor;
import com.sajitar.backend.domain.validation.profile.Name;
import com.sajitar.backend.domain.validation.profile.TwoFactor;

import jakarta.validation.constraints.NotNull;

@MasterRequiresTwoFactor
public record UpdateProfileCommand(
        @NotNull UUID id,
        @NotNull(message = "{validation.not-null}") Profile.Type type,
        @Name String name,
        @Description String description,
        @Birthday LocalDate birthday,
        @TwoFactor Boolean twoFactor,
        String password,
        String address) implements MasterRequiresTwoFactor.Pair {

    public UpdateProfileCommand(
            final UUID id,
            final Profile.Type type,
            final String name,
            final String description,
            final LocalDate birthday,
            final Boolean twoFactor) {
        this(id, type, name, description, birthday, twoFactor, null, null);
    }

}
