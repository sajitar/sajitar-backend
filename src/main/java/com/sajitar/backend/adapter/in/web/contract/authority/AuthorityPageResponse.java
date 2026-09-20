package com.sajitar.backend.adapter.in.web.contract.authority;

import java.util.List;

import com.sajitar.backend.application.page.Page;
import com.sajitar.backend.domain.model.authority.Authority;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AuthorityPage", description = "Página de authorities com metadados de paginação por cursor.")
public record AuthorityPageResponse(
        @Schema(description = "Itens da página atual")
        List<AuthorityResponse> content,
        @Schema(description = "Quantidade de itens antes do primeiro elemento na ordenação oposta")
        long precedingElements,
        @Schema(description = "Quantidade de itens após o último elemento da página")
        long followingElements,
        @Schema(description = "Indica se a ordenação é descendente")
        boolean reverse) {

    public static AuthorityPageResponse from(final Page<Authority> page) {
        return new AuthorityPageResponse(
                page.content().stream().map(AuthorityResponse::from).toList(),
                page.precedingElements(),
                page.followingElements(),
                page.reverse());
    }

}
