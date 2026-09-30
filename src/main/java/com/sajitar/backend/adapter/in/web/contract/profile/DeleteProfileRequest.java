package com.sajitar.backend.adapter.in.web.contract.profile;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sajitar.backend.application.command.profile.DeleteProfileCommand;
import com.sajitar.backend.domain.validation.checker.Code;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
        name = "DeleteProfileRequest",
        description = "Confere o código enviado ao e-mail vigente e conclui a exclusão.")
public record DeleteProfileRequest(
        @Schema(description = "Código de seis dígitos enviado ao e-mail vigente", example = "654321")
        @Code String code) {

    public DeleteProfileCommand toCommand(final UUID id, final UUID callerProfileId, final String address) {
        return new DeleteProfileCommand(id, callerProfileId, code, address);
    }

}
