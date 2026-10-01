package com.sajitar.backend.adapter.in.web.contract.profile;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sajitar.backend.adapter.in.web.ScalarAsStringDeserializer;
import com.sajitar.backend.application.command.profile.CreateProfileCommand;
import com.sajitar.backend.domain.model.profile.Profile;
import com.sajitar.backend.domain.validation.profile.Birthday;
import com.sajitar.backend.domain.validation.profile.Description;
import com.sajitar.backend.domain.validation.profile.Email;
import com.sajitar.backend.domain.validation.profile.Name;
import com.sajitar.backend.domain.validation.profile.Password;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
        name = "CreateProfileRequest",
        description = "Corpo da requisição para criação de perfil. O identificador é gerado pelo servidor. type omitido ou nulo grava WRITER. Só um caller MASTER autentica a escolha do tipo. twoFactor extra é ignorado; WRITER e READER nascem sem dupla autenticação; type MASTER nasce com twoFactor true.")
public record CreateProfileRequest(
        @Schema(description = "Tipo do perfil. Omitir ou null grava WRITER. Só MASTER autentica a escolha.", example = "WRITER")
        @JsonDeserialize(using = ScalarAsStringDeserializer.class)
        String type,
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

    public CreateProfileCommand toCommand() {
        return new CreateProfileCommand(type == null ? null : Profile.Type.parse(type), name, description, birthday, email, password);
    }

}
