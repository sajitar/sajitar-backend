package com.sajitar.backend.adapter.in.web.contract.profile;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sajitar.backend.application.command.profile.ConfirmChangeEmailCommand;
import com.sajitar.backend.domain.validation.checker.Code;
import com.sajitar.backend.domain.validation.profile.Email;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
        name = "ConfirmChangeEmailRequest",
        description = "Confere o código enviado ao e-mail vigente e grava o e-mail novo no payload.")
public record ConfirmChangeEmailRequest(
        @Schema(description = "Código de seis dígitos enviado ao e-mail vigente", example = "123456")
        @Code String code,
        @Schema(description = "Endereço de e-mail que passará a valer após a segunda confirmação", example = "nova@example.com")
        @Email String newEmail) {

    public ConfirmChangeEmailCommand toCommand(final UUID profileId, final String address) {
        return new ConfirmChangeEmailCommand(profileId, code, newEmail, address);
    }

}
