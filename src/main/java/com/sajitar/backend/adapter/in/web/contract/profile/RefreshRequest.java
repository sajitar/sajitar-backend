package com.sajitar.backend.adapter.in.web.contract.profile;

import com.sajitar.backend.application.command.profile.RefreshProfileCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "RefreshRequest", description = "Refresh JWT para emissão de um novo par de tokens.")
public record RefreshRequest(
        @Schema(description = "Refresh JWT", example = "eyJhbGciOiJIUzI1NiJ9...")
        @NotBlank(message = "{validation.not-blank}") String refreshToken) {

    public RefreshProfileCommand toCommand() {
        return new RefreshProfileCommand(refreshToken);
    }

}
