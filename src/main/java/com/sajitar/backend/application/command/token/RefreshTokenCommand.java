package com.sajitar.backend.application.command.token;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenCommand(@NotBlank(message = "{validation.not-blank}") String refreshToken) {
}
