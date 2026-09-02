package com.sajitar.backend.application.command;

import java.time.LocalDate;

import com.sajitar.backend.domain.validation.profile.Birthday;
import com.sajitar.backend.domain.validation.profile.Description;
import com.sajitar.backend.domain.validation.profile.Email;
import com.sajitar.backend.domain.validation.profile.Name;
import com.sajitar.backend.domain.validation.profile.Password;

public record CreateProfileCommand(
        @Name String name,
        @Description String description,
        @Birthday LocalDate birthday,
        @Email String email,
        @Password String password) {

}
