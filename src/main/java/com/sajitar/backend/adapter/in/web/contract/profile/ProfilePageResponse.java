package com.sajitar.backend.adapter.in.web.contract.profile;

import java.util.List;

import com.sajitar.backend.application.page.Page;
import com.sajitar.backend.domain.model.profile.Profile;

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

    public static ProfilePageResponse from(final Page<Profile> page) {
        return new ProfilePageResponse(
                page.content().stream().map(ProfileSummaryResponse::from).toList(),
                page.precedingElements(),
                page.followingElements(),
                page.reverse());
    }

}
