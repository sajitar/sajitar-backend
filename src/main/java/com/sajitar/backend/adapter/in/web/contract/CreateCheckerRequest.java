package com.sajitar.backend.adapter.in.web.contract;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sajitar.backend.adapter.in.web.ScalarAsStringDeserializer;
import com.sajitar.backend.application.command.CreateCheckerCommand;
import com.sajitar.backend.domain.model.Checker;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "CreateCheckerRequest", description = "Corpo da requisição para criação de checker. O identificador é gerado pelo servidor.")
public record CreateCheckerRequest(
        @Schema(description = "Tipo do desafio", example = "CHANGE_EMAIL")
        @JsonDeserialize(using = ScalarAsStringDeserializer.class)
        @NotNull(message = "{validation.not-null}")
        String type) {

    public CreateCheckerCommand toCommand(final UUID profileId) {
        return new CreateCheckerCommand(profileId, Checker.Type.parse(type));
    }

}
