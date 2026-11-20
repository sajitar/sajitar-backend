package com.sajitar.backend.adapter.in.web.contract.term;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sajitar.backend.adapter.in.web.PatchValueDeserializer;
import com.sajitar.backend.adapter.in.web.ScalarAsStringDeserializer;
import com.sajitar.backend.application.command.PatchValue;
import com.sajitar.backend.application.command.term.PatchTermCommand;
import com.sajitar.backend.domain.model.term.Term;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
        name = "PatchTermRequest",
        description = "Alteração de uma linha. type e content obrigatórios. draft opcional: ausente ou true mantém true; false publica se o id tem EN, PT e ES; null retorna 400.")
public record PatchTermRequest(
        @Schema(description = "Língua da linha a alterar", example = "EN")
        @JsonDeserialize(using = ScalarAsStringDeserializer.class)
        @NotNull(message = "{validation.not-null}")
        String type,
        @Schema(description = "Texto da linha (obrigatório, sem teto de caracteres)", example = "Terms of use.")
        @NotBlank(message = "{validation.not-blank}")
        String content,
        @Schema(description = "Rascunho. Omitir ou true mantém true; false publica; null retorna 400.", example = "false")
        @JsonDeserialize(using = PatchValueDeserializer.class)
        PatchValue<Boolean> draft) {

    public PatchTermCommand toCommand(final UUID id) {
        return new PatchTermCommand(id, Term.Type.parse(type), content, draft);
    }

}
