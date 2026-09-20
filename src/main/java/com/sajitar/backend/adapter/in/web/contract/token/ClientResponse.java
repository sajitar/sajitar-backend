package com.sajitar.backend.adapter.in.web.contract.token;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sajitar.backend.domain.model.token.Client;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Client", description = "User-Agent parseado no servidor. Omitido quando o parse falha.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClientResponse(
        @Schema(description = "Navegador ou aplicativo", example = "Chrome")
        String name,
        @Schema(description = "Sistema operacional", example = "Linux")
        String os,
        @Schema(
                description = "Classe do dispositivo",
                example = "desktop",
                allowableValues = { "desktop", "mobile", "tablet", "unknown" })
        String device) {

    public static ClientResponse from(final Client client) {
        if (client == null) {
            return null;
        }
        return new ClientResponse(
                client.name(),
                client.os(),
                client.device() == null ? null : client.device().value());
    }

}
