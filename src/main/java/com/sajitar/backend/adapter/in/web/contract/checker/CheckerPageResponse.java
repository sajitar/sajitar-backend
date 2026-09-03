package com.sajitar.backend.adapter.in.web.contract.checker;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sajitar.backend.domain.model.checker.Checker;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CheckerPage", description = "Página de checkers de um perfil, com cursor por tipo.")
public record CheckerPageResponse(
        @Schema(description = "Tamanho máximo solicitado da página", example = "100")
        int limit,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(description = "Cursor informado na consulta (omitido quando ausente)", example = "CHANGE_EMAIL")
        Checker.Type lastSeenType,
        @Schema(description = "Itens da página atual")
        List<CheckerResponse> content) {

    public static CheckerPageResponse from(
            final int limit,
            final Checker.Type lastSeenType,
            final List<Checker> content) {
        return new CheckerPageResponse(
                limit,
                lastSeenType,
                content.stream().map(CheckerResponse::from).toList());
    }

}
