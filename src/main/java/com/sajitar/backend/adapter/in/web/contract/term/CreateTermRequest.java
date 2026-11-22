package com.sajitar.backend.adapter.in.web.contract.term;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sajitar.backend.adapter.in.web.ScalarAsStringDeserializer;
import com.sajitar.backend.application.command.term.CreateTermCommand;
import com.sajitar.backend.application.command.term.CreateTermTypeCommand;
import com.sajitar.backend.domain.model.term.TermDetail;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
        name = "CreateTermRequest",
        description = "Corpo da criação de linha. O id é gerado (primeira linha) ou vem da URL (acréscimo). Só type e content são aceitos; draft nasce true.")
public record CreateTermRequest(
        @Schema(description = "Língua da linha", example = "EN")
        @JsonDeserialize(using = ScalarAsStringDeserializer.class)
        @NotNull(message = "{validation.not-null}")
        String type,
        @Schema(description = "Texto da linha (obrigatório, sem teto de caracteres)", example = "Terms of use.")
        @NotBlank(message = "{validation.not-blank}")
        String content) {

    public CreateTermCommand toCommand() {
        return new CreateTermCommand(TermDetail.Type.parse(type), content);
    }

    public CreateTermTypeCommand toCommand(final UUID id) {
        return new CreateTermTypeCommand(id, TermDetail.Type.parse(type), content);
    }

}
