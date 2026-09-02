package com.sajitar.backend.adapter.in.web.contract;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sajitar.backend.application.command.UpdateProfileCommand;
import com.sajitar.backend.domain.validation.profile.Birthday;
import com.sajitar.backend.domain.validation.profile.Description;
import com.sajitar.backend.domain.validation.profile.Email;
import com.sajitar.backend.domain.validation.profile.Name;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
        name = "UpdateProfileRequest",
        description = "Corpo da requisição para atualização completa de perfil. O identificador não é aceito no corpo.")
public record UpdateProfileRequest(
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

    public UpdateProfileCommand toCommand(final UUID id) {
        return new UpdateProfileCommand(id, name, description, birthday, email, password);
    }

}
