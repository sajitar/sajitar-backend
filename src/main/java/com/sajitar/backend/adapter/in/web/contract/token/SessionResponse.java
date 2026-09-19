package com.sajitar.backend.adapter.in.web.contract.token;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Session", description = "Sessão de login ativa. O instante do login está nos 48 bits de tempo do id.")
public record SessionResponse(
        @Schema(description = "Identificador da sessão de login", example = "018f3c2a-7b00-7c3d-9e1a-000000000010")
        UUID id,
        @Schema(description = "Indica a sessão do Bearer usado nesta requisição", example = "true")
        boolean current) {

}
