package com.sajitar.backend.adapter.in.web.contract;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sajitar.backend.application.command.PatchCheckerCommand;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
        name = "PatchCheckerRequest",
        description = "Corpo da atualização parcial. Campos omitidos ou nulos permanecem inalterados. O identificador não é aceito no corpo.")
public record PatchCheckerRequest(
        @Schema(description = "Código de 6 dígitos. Omitir ou null mantém o atual.", example = "123456")
        String code,
        @Schema(description = "Carga opcional. Omitir ou null mantém a atual.", example = "novo@example.com")
        String payload,
        @Schema(description = "Tentativas restantes (0–10). Omitir ou null mantém o atual.", example = "7")
        Integer attempts,
        @Schema(description = "Substituições restantes (0–3). Omitir ou null mantém o atual.", example = "1")
        Integer replaces) {

    public PatchCheckerCommand toCommand(final UUID id) {
        return new PatchCheckerCommand(id, code, payload, attempts, replaces);
    }

}
