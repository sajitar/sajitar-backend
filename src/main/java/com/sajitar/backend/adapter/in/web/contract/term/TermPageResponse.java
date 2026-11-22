package com.sajitar.backend.adapter.in.web.contract.term;

import java.util.List;

import com.sajitar.backend.application.page.Page;
import com.sajitar.backend.domain.model.term.TermDetail;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TermPage", description = "Página de Term com paginação por cursor. Cada item representa um id.")
public record TermPageResponse(
        @Schema(description = "Itens da página atual; cada item representa um id")
        List<TermResponse> content,
        @Schema(description = "Quantidade de ids antes do primeiro elemento na ordenação oposta")
        long precedingElements,
        @Schema(description = "Quantidade de ids após o último elemento da página")
        long followingElements,
        @Schema(description = "Indica se a ordenação é descendente")
        boolean reverse) {

    public static TermPageResponse from(final Page<List<TermDetail>> page, final TermDetail.Type lang) {
        return new TermPageResponse(
                page.content().stream().map(lines -> TermResponse.of(lines, lang, true)).toList(),
                page.precedingElements(),
                page.followingElements(),
                page.reverse());
    }

}
