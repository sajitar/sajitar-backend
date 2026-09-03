package com.sajitar.backend.adapter.in.web.contract.authority;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sajitar.backend.adapter.in.web.ScalarAsStringDeserializer;
import com.sajitar.backend.application.command.authority.CreateAuthorityCommand;
import com.sajitar.backend.domain.model.authority.Authority;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "CreateAuthorityRequest", description = "Corpo da criação. O identificador é gerado pelo servidor. Só type é aceito.")
public record CreateAuthorityRequest(
        @Schema(description = "Tipo da authority", example = "MASTER")
        @JsonDeserialize(using = ScalarAsStringDeserializer.class)
        @NotNull(message = "{validation.not-null}")
        String type) {

    public CreateAuthorityCommand toCommand(final UUID profileId) {
        return new CreateAuthorityCommand(profileId, Authority.Type.parse(type));
    }

}
