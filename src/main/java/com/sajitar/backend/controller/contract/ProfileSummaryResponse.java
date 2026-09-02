package com.sajitar.backend.controller.contract;

import java.util.UUID;

import com.sajitar.backend.domain.model.Profile;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ProfileSummary", description = "Visão resumida de um perfil exposta nas listagens e respostas de escrita.")
public record ProfileSummaryResponse(
        @Schema(description = "Identificador único do perfil", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,
        @Schema(description = "Nome do perfil", example = "Maria Silva")
        String name,
        @Schema(description = "Descrição do perfil", example = "Uma pessoa criativa e dedicada.")
        String description) {

    public static ProfileSummaryResponse from(final Profile profile) {
        return new ProfileSummaryResponse(profile.getId(), profile.getName(), profile.getDescription());
    }

}
