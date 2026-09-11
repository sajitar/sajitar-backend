package com.sajitar.backend.application.command.profile;

import jakarta.validation.constraints.NotBlank;

public record RefreshProfileCommand(@NotBlank(message = "{validation.not-blank}") String refreshToken) {
}
