package com.sajitar.backend.adapter.in.web.contract.note;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sajitar.backend.adapter.in.web.ScalarAsStringDeserializer;
import com.sajitar.backend.application.command.note.UpdateNoteCommand;
import com.sajitar.backend.domain.model.note.Note;
import com.sajitar.backend.domain.validation.note.Content;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
        name = "UpdateNoteRequest",
        description = "Substituição de type e content. O identificador não é aceito no corpo.")
public record UpdateNoteRequest(
        @Schema(description = "Tipo da nota", example = "PUBLIC")
        @JsonDeserialize(using = ScalarAsStringDeserializer.class)
        @NotNull(message = "{validation.not-null}")
        String type,
        @Schema(description = "Texto da nota (obrigatório, até 1000 caracteres)", example = "Texto atualizado.")
        @Content
        String content) {

    public UpdateNoteCommand toCommand(final UUID id) {
        return new UpdateNoteCommand(id, Note.Type.parse(type), content);
    }

}
