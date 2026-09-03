package com.sajitar.backend.adapter.in.web.contract.checker;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sajitar.backend.adapter.in.web.ScalarAsStringDeserializer;
import com.sajitar.backend.application.command.checker.PatchCheckerCommand;
import com.sajitar.backend.domain.model.checker.Checker;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
        name = "PatchCheckerRequest",
        description = "Atualização parcial de type e payload. Campos omitidos ou nulos permanecem inalterados. O identificador não é aceito no corpo.")
public record PatchCheckerRequest(
        @Schema(description = "Tipo do desafio. Omitir ou null mantém o atual.", example = "CHANGE_EMAIL")
        @JsonDeserialize(using = ScalarAsStringDeserializer.class)
        String type,
        @Schema(description = "Carga opcional. Omitir ou null mantém a atual.", example = "novo@example.com")
        String payload) {

    public PatchCheckerCommand toCommand(final UUID id) {
        return new PatchCheckerCommand(id, type == null ? null : Checker.Type.parse(type), payload);
    }

}
