package com.sajitar.backend.controller.contract;

import java.util.List;

import com.sajitar.backend.domain.model.Profile;
import com.sajitar.backend.util.Pagination;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ProfilePage", description = "Página de perfis com metadados de paginação por cursor.")
public record ProfilePageResponse(
        @Schema(description = "Itens da página atual")
        List<ProfileSummaryResponse> content,
        @Schema(description = "Quantidade de itens antes do primeiro elemento na ordenação oposta")
        long precedingElements,
        @Schema(description = "Quantidade de itens após o último elemento da página")
        long followingElements,
        @Schema(description = "Indica se a ordenação é descendente")
        boolean reverse) {

    public static ProfilePageResponse from(final Pagination<Profile> pagination) {
        return new ProfilePageResponse(
                pagination.content().stream().map(ProfileSummaryResponse::from).toList(),
                pagination.precedingElements(),
                pagination.followingElements(),
                pagination.reverse());
    }

}
