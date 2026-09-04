package com.sajitar.backend.adapter.in.web.contract.note;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sajitar.backend.adapter.in.web.PatchValueDeserializer;
import com.sajitar.backend.adapter.in.web.ScalarAsStringDeserializer;
import com.sajitar.backend.application.command.PatchValue;
import com.sajitar.backend.application.command.note.PatchNoteCommand;
import com.sajitar.backend.domain.model.note.Note;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
        name = "PatchNoteRequest",
        description = "Atualização parcial de type e content. Campo omitido permanece inalterado. content nulo ou em branco é inválido. O identificador não é aceito no corpo.")
public record PatchNoteRequest(
        @Schema(description = "Tipo da nota. Omitir ou null mantém o atual.", example = "PROTECTED")
        @JsonDeserialize(using = ScalarAsStringDeserializer.class)
        String type,
        @Schema(description = "Texto da nota. Omitir para manter; null ou vazio retorna 400.", example = "Texto parcial.")
        @JsonDeserialize(using = PatchValueDeserializer.class)
        PatchValue<String> content) {

    public PatchNoteCommand toCommand(final UUID id) {
        return new PatchNoteCommand(id, type == null ? null : Note.Type.parse(type), content);
    }

}
