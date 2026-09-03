package com.sajitar.backend.adapter.in.web.contract.authority;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sajitar.backend.adapter.in.web.ScalarAsStringDeserializer;
import com.sajitar.backend.application.command.authority.PatchAuthorityCommand;
import com.sajitar.backend.domain.model.authority.Authority;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
        name = "PatchAuthorityRequest",
        description = "Atualização parcial de type. Campo omitido ou nulo permanece inalterado. O identificador não é aceito no corpo.")
public record PatchAuthorityRequest(
        @Schema(description = "Tipo da authority. Omitir ou null mantém o atual.", example = "MASTER")
        @JsonDeserialize(using = ScalarAsStringDeserializer.class)
        String type) {

    public PatchAuthorityCommand toCommand(final UUID id) {
        return new PatchAuthorityCommand(id, type == null ? null : Authority.Type.parse(type));
    }

}
