package com.sajitar.backend.application.command.profile;

import com.sajitar.backend.domain.validation.checker.Code;
import com.sajitar.backend.domain.validation.profile.Email;
import com.sajitar.backend.domain.validation.profile.Password;

public record ConfirmPasswordRecoveryCommand(
        @Email String email,
        @Code String code,
        @Password String newPassword,
        String address) {
}
