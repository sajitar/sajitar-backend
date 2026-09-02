package com.sajitar.backend.adapter.in.web.contract;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "ValidationError",
        description = "Erros de validação agrupados por nome de propriedade.",
        example = "{\"email\": [\"must be a well-formed email address\"]}")
public record ValidationErrorResponse(
        @Schema(
                description = "Mapa de propriedade inválida para lista de mensagens",
                example = "{\"email\": [\"must be a well-formed email address\"]}")
        Map<String, List<String>> errors) {

}
