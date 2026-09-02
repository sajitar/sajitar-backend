package com.sajitar.backend.application.command;

import java.time.LocalDate;
import java.util.UUID;

import com.sajitar.backend.domain.validation.profile.Birthday;
import com.sajitar.backend.domain.validation.profile.Description;
import com.sajitar.backend.domain.validation.profile.Email;
import com.sajitar.backend.domain.validation.profile.Name;

import jakarta.validation.constraints.NotNull;

public record UpdateProfileCommand(
        @NotNull UUID id,
        @Name String name,
        @Description String description,
        @Birthday LocalDate birthday,
        @Email String email,
        String password) {

    public boolean hasNewPassword() {
        return password != null && !password.isBlank();
    }

}
