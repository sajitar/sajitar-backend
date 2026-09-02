package com.sajitar.backend.adapter.in.web.contract;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sajitar.backend.adapter.in.web.PatchValueDeserializer;
import com.sajitar.backend.application.command.PatchProfileCommand;
import com.sajitar.backend.application.command.PatchValue;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
        name = "PatchProfileRequest",
        description = "Corpo da atualização parcial. Campos omitidos permanecem inalterados. O identificador não é aceito no corpo.")
public record PatchProfileRequest(
        @Schema(description = "Nome do perfil. Omitir para manter o atual.", example = "Maria Silva")
        @JsonDeserialize(using = PatchValueDeserializer.class)
        PatchValue<String> name,
        @Schema(description = "Descrição do perfil. Omitir para manter; null remove a descrição.", example = "Uma pessoa criativa e dedicada.")
        @JsonDeserialize(using = PatchValueDeserializer.class)
        PatchValue<String> description,
        @Schema(description = "Data de nascimento. Omitir para manter a atual.", example = "1988-01-10")
        @JsonDeserialize(using = PatchValueDeserializer.class)
        PatchValue<LocalDate> birthday,
        @Schema(description = "Endereço de e-mail. Omitir para manter o atual.", example = "maria@example.com")
        @JsonDeserialize(using = PatchValueDeserializer.class)
        PatchValue<String> email,
        @Schema(
                description = "Nova senha em texto plano. Omitida, nula ou em branco mantém a senha atual.",
                example = "novaSenhaSegura1")
        @JsonDeserialize(using = PatchValueDeserializer.class)
        PatchValue<String> password) {

    public PatchProfileCommand toCommand(final UUID id) {
        return new PatchProfileCommand(id, name, description, birthday, email, password);
    }

}
