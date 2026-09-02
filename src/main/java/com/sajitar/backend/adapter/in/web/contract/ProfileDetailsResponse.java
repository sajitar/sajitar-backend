package com.sajitar.backend.adapter.in.web.contract;

import java.time.LocalDate;
import java.util.UUID;

import com.sajitar.backend.domain.model.Profile;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ProfileDetails", description = "Detalhes completos de um perfil, incluindo campos sensíveis de leitura.")
public record ProfileDetailsResponse(
        @Schema(description = "Identificador único do perfil", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,
        @Schema(description = "Nome do perfil", example = "Maria Silva")
        String name,
        @Schema(description = "Descrição do perfil", example = "Uma pessoa criativa e dedicada.")
        String description,
        @Schema(description = "Data de nascimento", example = "1988-01-10")
        LocalDate birthday,
        @Schema(description = "Endereço de e-mail", example = "maria@example.com")
        String email) {

    public static ProfileDetailsResponse from(final Profile profile) {
        return new ProfileDetailsResponse(
                profile.id(),
                profile.name(),
                profile.description(),
                profile.birthday(),
                profile.email());
    }

}
