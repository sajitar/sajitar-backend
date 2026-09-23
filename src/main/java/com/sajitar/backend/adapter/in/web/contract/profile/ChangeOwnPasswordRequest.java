package com.sajitar.backend.adapter.in.web.contract.profile;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sajitar.backend.application.command.profile.ChangeOwnPasswordCommand;
import com.sajitar.backend.domain.validation.profile.DifferentPasswords;
import com.sajitar.backend.domain.validation.profile.Password;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@DifferentPasswords
@Schema(
        name = "ChangeOwnPasswordRequest",
        description = "Troca a senha do perfil autenticado. Exige a senha atual.")
public record ChangeOwnPasswordRequest(
        @Schema(description = "Senha vigente em texto plano", example = "senhaAtual12")
        @Password String currentPassword,
        @Schema(description = "Nova senha em texto plano", example = "senhaNovaSegura1")
        @Password String newPassword) implements DifferentPasswords.Pair {

    public ChangeOwnPasswordCommand toCommand(final UUID profileId, final String address) {
        return new ChangeOwnPasswordCommand(profileId, currentPassword, newPassword, address);
    }

}
