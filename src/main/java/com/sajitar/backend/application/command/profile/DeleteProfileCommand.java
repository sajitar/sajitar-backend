package com.sajitar.backend.application.command.profile;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record DeleteProfileCommand(@NotNull UUID id) {
}
