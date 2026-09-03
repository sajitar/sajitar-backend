package com.sajitar.backend.adapter.in.web.contract.authority;

import java.util.UUID;

import com.sajitar.backend.domain.model.authority.Authority;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Authority", description = "Representação de uma authority atribuída a um perfil.")
public record AuthorityResponse(
        @Schema(description = "Identificador único da authority", example = "019c2000-a111-7000-8000-111111111111")
        UUID id,
        @Schema(description = "Identificador do perfil associado", example = "01989bad-6161-7000-0ae9-f440b10578ec")
        UUID profileId,
        @Schema(description = "Tipo da authority", example = "MASTER")
        Authority.Type type) {

    public static AuthorityResponse from(final Authority authority) {
        return new AuthorityResponse(authority.id(), authority.profileId(), authority.type());
    }

}
