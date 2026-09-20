package com.sajitar.backend.adapter.in.web.contract.authority;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sajitar.backend.adapter.in.web.ScalarAsStringDeserializer;
import com.sajitar.backend.application.command.authority.UpdateAuthorityCommand;
import com.sajitar.backend.domain.model.authority.Authority;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
        name = "UpdateAuthorityRequest",
        description = "Substituição de type. O identificador não é aceito no corpo.")
public record UpdateAuthorityRequest(
        @Schema(description = "Tipo da authority", example = "MASTER")
        @JsonDeserialize(using = ScalarAsStringDeserializer.class)
        @NotNull(message = "{validation.not-null}")
        String type) {

    public UpdateAuthorityCommand toCommand(final UUID id) {
        return new UpdateAuthorityCommand(id, Authority.Type.parse(type));
    }

}
