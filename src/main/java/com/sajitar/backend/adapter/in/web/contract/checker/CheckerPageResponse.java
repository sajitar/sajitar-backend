package com.sajitar.backend.adapter.in.web.contract.checker;

import java.util.List;

import com.sajitar.backend.application.page.Page;
import com.sajitar.backend.domain.model.checker.Checker;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CheckerPage", description = "Página de checkers com metadados de paginação por cursor.")
public record CheckerPageResponse(
        @Schema(description = "Itens da página atual")
        List<CheckerResponse> content,
        @Schema(description = "Quantidade de itens antes do primeiro elemento na ordenação oposta")
        long precedingElements,
        @Schema(description = "Quantidade de itens após o último elemento da página")
        long followingElements,
        @Schema(description = "Indica se a ordenação é descendente")
        boolean reverse) {

    public static CheckerPageResponse from(final Page<Checker> page) {
        return new CheckerPageResponse(
                page.content().stream().map(CheckerResponse::from).toList(),
                page.precedingElements(),
                page.followingElements(),
                page.reverse());
    }

}
