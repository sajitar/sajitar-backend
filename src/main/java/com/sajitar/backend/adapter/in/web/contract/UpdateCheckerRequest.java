package com.sajitar.backend.adapter.in.web.contract;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sajitar.backend.application.command.UpdateCheckerCommand;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
        name = "UpdateCheckerRequest",
        description = "Corpo da substituição dos campos mutáveis. Campos omitidos ou nulos voltam aos defaults de criação. O identificador não é aceito no corpo.")
public record UpdateCheckerRequest(
        @Schema(description = "Código de 6 dígitos. Omitido ou nulo gera um novo código.", example = "123456")
        String code,
        @Schema(description = "Carga opcional. Omitida ou nula limpa o valor.", example = "novo@example.com")
        String payload,
        @Schema(description = "Tentativas restantes (0–10). Omitido ou nulo volta a 10.", example = "10")
        Integer attempts,
        @Schema(description = "Substituições restantes (0–3). Omitido ou nulo volta a 3.", example = "3")
        Integer replaces) {

    public UpdateCheckerCommand toCommand(final UUID id) {
        return new UpdateCheckerCommand(id, code, payload, attempts, replaces);
    }

}
