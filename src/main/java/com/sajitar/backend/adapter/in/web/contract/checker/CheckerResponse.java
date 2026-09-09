package com.sajitar.backend.adapter.in.web.contract.checker;

import java.time.Instant;
import java.util.UUID;

import com.sajitar.backend.domain.model.checker.Checker;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Checker", description = "Representação completa de um checker, incluindo código e payload.")
public record CheckerResponse(
        @Schema(description = "Identificador único do checker", example = "019c1000-a111-7000-8000-111111111111")
        UUID id,
        @Schema(description = "Identificador do perfil associado", example = "01989bad-6161-7000-0ae9-f440b10578ec")
        UUID profileId,
        @Schema(description = "Tipo do desafio", example = "CHANGE_EMAIL")
        Checker.Type type,
        @Schema(description = "Código de 6 dígitos", example = "123456")
        String code,
        @Schema(description = "Carga opcional associada ao desafio", example = "novo@example.com")
        String payload,
        @Schema(description = "Substituições restantes", example = "3")
        int replaces,
        @Schema(description = "Tentativas restantes", example = "10")
        int attempts,
        @Schema(description = "Instante da última atualização", example = "2001-04-24T21:00:00Z")
        Instant updatedAt,
        @Schema(description = "Indica se o payload ainda precisa ser informado", example = "true")
        boolean requiredPayload) {

    public static CheckerResponse from(final Checker checker) {
        return new CheckerResponse(
                checker.id(),
                checker.profileId(),
                checker.type(),
                checker.code(),
                checker.payload(),
                checker.replaces(),
                checker.attempts(),
                checker.updatedAt(),
                checker.requiredPayload());
    }

}
