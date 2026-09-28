package com.sajitar.backend.application.command.profile;

import java.time.LocalDate;
import java.util.UUID;

import com.sajitar.backend.domain.model.profile.Profile;
import com.sajitar.backend.domain.validation.profile.Birthday;
import com.sajitar.backend.domain.validation.profile.Description;
import com.sajitar.backend.domain.validation.profile.Name;

import jakarta.validation.constraints.NotNull;

public record UpdateProfileCommand(
        @NotNull UUID id,
        @NotNull(message = "{validation.not-null}") Profile.Type type,
        @Name String name,
        @Description String description,
        @Birthday LocalDate birthday) {

}
