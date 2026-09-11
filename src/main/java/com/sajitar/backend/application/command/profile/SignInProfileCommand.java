package com.sajitar.backend.application.command.profile;

import com.sajitar.backend.domain.validation.profile.Email;
import com.sajitar.backend.domain.validation.profile.Password;

public record SignInProfileCommand(@Email String email, @Password String password) {}
