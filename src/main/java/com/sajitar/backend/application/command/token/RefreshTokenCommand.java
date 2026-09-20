package com.sajitar.backend.application.command.token;

import com.sajitar.backend.domain.model.token.Client;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenCommand(
        @NotBlank(message = "{validation.not-blank}") String refreshToken,
        String address,
        Client client) {
}
