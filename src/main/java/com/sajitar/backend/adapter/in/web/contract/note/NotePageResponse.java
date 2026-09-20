package com.sajitar.backend.adapter.in.web.contract.note;

import java.util.List;

import com.sajitar.backend.application.page.Page;
import com.sajitar.backend.domain.model.note.Note;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "NotePage", description = "Página de notas com metadados de paginação por cursor.")
public record NotePageResponse(
        @Schema(description = "Itens da página atual")
        List<NoteResponse> content,
        @Schema(description = "Quantidade de itens antes do primeiro elemento na ordenação oposta")
        long precedingElements,
        @Schema(description = "Quantidade de itens após o último elemento da página")
        long followingElements,
        @Schema(description = "Indica se a ordenação é descendente")
        boolean reverse) {

    public static NotePageResponse from(final Page<Note> page) {
        return new NotePageResponse(
                page.content().stream().map(NoteResponse::from).toList(),
                page.precedingElements(),
                page.followingElements(),
                page.reverse());
    }

}
