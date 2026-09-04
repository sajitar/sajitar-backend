package com.sajitar.backend.adapter.in.web.contract.note;

import java.util.UUID;

import com.sajitar.backend.domain.model.note.Note;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Note", description = "Representação de uma nota atribuída a um perfil.")
public record NoteResponse(
        @Schema(description = "Identificador único da nota", example = "019c3000-a111-7000-8000-111111111111")
        UUID id,
        @Schema(description = "Identificador do perfil associado", example = "01989bad-6161-7000-0ae9-f440b10578ec")
        UUID profileId,
        @Schema(description = "Tipo da nota", example = "PUBLIC")
        Note.Type type,
        @Schema(description = "Texto da nota", example = "Alice public one")
        String content) {

    public static NoteResponse from(final Note note) {
        return new NoteResponse(note.id(), note.profileId(), note.type(), note.content());
    }

}
