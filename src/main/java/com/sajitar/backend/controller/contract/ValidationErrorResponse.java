package com.sajitar.backend.controller.contract;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "ValidationError",
        description = "Erros de validação agrupados por nome de propriedade.",
        example = "{\"email\": [\"deve ser um endereço de e-mail bem formado\"]}")
public record ValidationErrorResponse(
        @Schema(
                description = "Mapa de propriedade inválida para lista de mensagens",
                example = "{\"email\": [\"deve ser um endereço de e-mail bem formado\"]}")
        Map<String, List<String>> errors) {

}
