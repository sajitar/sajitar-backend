package com.sajitar.backend.application.command.profile;

import java.time.LocalDate;

import com.sajitar.backend.domain.model.profile.Profile;
import com.sajitar.backend.domain.validation.profile.Birthday;
import com.sajitar.backend.domain.validation.profile.Description;
import com.sajitar.backend.domain.validation.profile.Email;
import com.sajitar.backend.domain.validation.profile.Name;
import com.sajitar.backend.domain.validation.profile.Password;

import jakarta.validation.constraints.NotNull;

public record CreateProfileCommand(
        @NotNull(message = "{validation.not-null}") Profile.Type type,
        @Name String name,
        @Description String description,
        @Birthday LocalDate birthday,
        @Email String email,
        @Password String password) {

}
