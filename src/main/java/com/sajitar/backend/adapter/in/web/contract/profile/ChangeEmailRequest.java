package com.sajitar.backend.adapter.in.web.contract.profile;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sajitar.backend.application.command.profile.ChangeEmailCommand;
import com.sajitar.backend.domain.validation.checker.Code;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
        name = "ChangeEmailRequest",
        description = "Confere o código enviado ao e-mail novo e conclui a troca.")
public record ChangeEmailRequest(
        @Schema(description = "Código de seis dígitos enviado ao e-mail novo", example = "654321")
        @Code String code) {

    public ChangeEmailCommand toCommand(final UUID profileId, final String address) {
        return new ChangeEmailCommand(profileId, code, address);
    }

}
