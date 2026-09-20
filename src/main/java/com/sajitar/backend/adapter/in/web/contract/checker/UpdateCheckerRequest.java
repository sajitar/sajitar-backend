package com.sajitar.backend.adapter.in.web.contract.checker;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sajitar.backend.adapter.in.web.ScalarAsStringDeserializer;
import com.sajitar.backend.application.command.checker.UpdateCheckerCommand;
import com.sajitar.backend.domain.model.checker.Checker;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
        name = "UpdateCheckerRequest",
        description = "Substituição de type e payload. O identificador não é aceito no corpo. Payload omitido ou nulo limpa o valor.")
public record UpdateCheckerRequest(
        @Schema(description = "Tipo do desafio", example = "CHANGE_EMAIL")
        @JsonDeserialize(using = ScalarAsStringDeserializer.class)
        @NotNull(message = "{validation.not-null}")
        String type,
        @Schema(description = "Carga opcional. Omitida ou nula limpa o valor.", example = "novo@example.com")
        String payload) {

    public UpdateCheckerCommand toCommand(final UUID id) {
        return new UpdateCheckerCommand(id, Checker.Type.parse(type), payload);
    }

}
