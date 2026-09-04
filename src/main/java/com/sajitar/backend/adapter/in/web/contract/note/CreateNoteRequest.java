package com.sajitar.backend.adapter.in.web.contract.note;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sajitar.backend.adapter.in.web.ScalarAsStringDeserializer;
import com.sajitar.backend.application.command.note.CreateNoteCommand;
import com.sajitar.backend.domain.model.note.Note;
import com.sajitar.backend.domain.validation.note.Content;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "CreateNoteRequest", description = "Corpo da criação. O identificador é gerado pelo servidor. Só type e content são aceitos.")
public record CreateNoteRequest(
        @Schema(description = "Tipo da nota", example = "PUBLIC")
        @JsonDeserialize(using = ScalarAsStringDeserializer.class)
        @NotNull(message = "{validation.not-null}")
        String type,
        @Schema(description = "Texto da nota (obrigatório, até 1000 caracteres)", example = "Uma nota pública.")
        @Content
        String content) {

    public CreateNoteCommand toCommand(final UUID profileId) {
        return new CreateNoteCommand(profileId, Note.Type.parse(type), content);
    }

}
