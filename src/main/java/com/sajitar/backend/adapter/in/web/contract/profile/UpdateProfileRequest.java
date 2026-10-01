package com.sajitar.backend.adapter.in.web.contract.profile;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sajitar.backend.adapter.in.web.ScalarAsStringDeserializer;
import com.sajitar.backend.application.command.profile.UpdateProfileCommand;
import com.sajitar.backend.domain.model.profile.Profile;
import com.sajitar.backend.domain.validation.profile.Birthday;
import com.sajitar.backend.domain.validation.profile.Description;
import com.sajitar.backend.domain.validation.profile.Name;
import com.sajitar.backend.domain.validation.profile.TwoFactor;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
        name = "UpdateProfileRequest",
        description = "Corpo da requisição para atualização completa de perfil. O identificador não é aceito no corpo. Senha e e-mail extras são ignorados. type e twoFactor são obrigatórios. Só um caller MASTER substitui o type vigente por um valor diferente. twoFactor só o próprio perfil grava. MASTER com twoFactor false responde 400.")
public record UpdateProfileRequest(
        @Schema(description = "Tipo do perfil", example = "WRITER")
        @JsonDeserialize(using = ScalarAsStringDeserializer.class)
        @NotNull(message = "{validation.not-null}")
        String type,
        @Schema(description = "Nome do perfil", example = "Maria Silva")
        @Name String name,
        @Schema(description = "Descrição opcional do perfil", example = "Uma pessoa criativa e dedicada.")
        @Description String description,
        @Schema(description = "Data de nascimento (idade mínima configurável no servidor)", example = "1988-01-10")
        @Birthday LocalDate birthday,
        @Schema(description = "Quando true, o signin por e-mail e senha exige o código de SIGN_IN. MASTER não aceita false.", example = "false")
        @TwoFactor Boolean twoFactor) {

    public UpdateProfileCommand toCommand(final UUID id) {
        return new UpdateProfileCommand(id, Profile.Type.parse(type), name, description, birthday, twoFactor);
    }

}
