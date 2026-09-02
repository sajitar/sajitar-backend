package com.sajitar.backend.controller.contract;

import java.time.LocalDate;

import com.sajitar.backend.domain.model.Profile;
import com.sajitar.backend.domain.validation.profile.Birthday;
import com.sajitar.backend.domain.validation.profile.Description;
import com.sajitar.backend.domain.validation.profile.Email;
import com.sajitar.backend.domain.validation.profile.Name;
import com.sajitar.backend.domain.validation.profile.Password;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "CreateProfileRequest",
        description = "Corpo da requisição para criação de perfil. O identificador é gerado pelo servidor.")
public record CreateProfileRequest(
        @Schema(description = "Nome do perfil", example = "Maria Silva")
        @Name String name,
        @Schema(description = "Descrição opcional do perfil", example = "Uma pessoa criativa e dedicada.")
        @Description String description,
        @Schema(description = "Data de nascimento (idade mínima configurável no servidor)", example = "1988-01-10")
        @Birthday LocalDate birthday,
        @Schema(description = "Endereço de e-mail (único no sistema)", example = "maria@example.com")
        @Email String email,
        @Schema(description = "Senha em texto plano (será codificada pelo servidor)", example = "senhaSegura1")
        @Password String password) {

    public Profile toProfile() {
        return Profile.builder()
                .name(name)
                .description(description)
                .birthday(birthday)
                .email(email)
                .password(password)
                .build();
    }

}
