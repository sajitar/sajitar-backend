package com.sajitar.backend.application.command.profile;

import com.sajitar.backend.domain.validation.profile.Email;

public record RequestPasswordRecoveryCommand(
        @Email String email,
        String address) {
}
