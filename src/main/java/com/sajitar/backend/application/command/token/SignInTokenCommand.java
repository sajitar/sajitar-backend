package com.sajitar.backend.application.command.token;

import com.sajitar.backend.domain.validation.profile.Email;
import com.sajitar.backend.domain.validation.profile.Password;

public record SignInTokenCommand(@Email String email, @Password String password, boolean refresh) {}
