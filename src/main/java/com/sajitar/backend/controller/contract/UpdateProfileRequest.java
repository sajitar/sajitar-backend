package com.sajitar.backend.controller.contract;

import java.time.LocalDate;
import java.util.UUID;

import com.sajitar.backend.domain.model.Profile;
import com.sajitar.backend.domain.validation.profile.Birthday;
import com.sajitar.backend.domain.validation.profile.Description;
import com.sajitar.backend.domain.validation.profile.Email;
import com.sajitar.backend.domain.validation.profile.Name;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(
        name = "UpdateProfileRequest",
        description = "Corpo da requisição para atualização de perfil existente.")
public record UpdateProfileRequest(
        @Schema(description = "Identificador do perfil a atualizar", example = "550e8400-e29b-41d4-a716-446655440000")
        @NotNull UUID id,
        @Schema(description = "Nome do perfil", example = "Maria Silva")
        @Name String name,
        @Schema(description = "Descrição opcional do perfil", example = "Uma pessoa criativa e dedicada.")
        @Description String description,
        @Schema(description = "Data de nascimento (idade mínima configurável no servidor)", example = "1988-01-10")
        @Birthday LocalDate birthday,
        @Schema(description = "Endereço de e-mail (único no sistema)", example = "maria@example.com")
        @Email String email,
        @Schema(
                description = "Nova senha em texto plano. Quando omitida ou em branco, a senha atual é mantida.",
                example = "novaSenhaSegura1")
        String password) {

    public Profile toProfile() {
        return Profile.builder()
                .id(id)
                .name(name)
                .description(description)
                .birthday(birthday)
                .email(email)
                .password(password)
                .build();
    }

}
