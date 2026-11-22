package com.sajitar.backend.adapter.in.web.contract.term;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sajitar.backend.domain.model.term.TermDetail;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "Term",
        description = "Representação de um Term. Com a query type, ou lang no vigente: id, type, content e draft. Sem essa query: id e contents.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TermResponse(
        @Schema(description = "Identificador do Term", example = "019c4000-a111-7000-8000-111111111111")
        UUID id,
        @Schema(description = "Língua da linha (presente só com a query type, ou lang no vigente)", example = "EN")
        TermDetail.Type type,
        @Schema(description = "Texto da linha (presente só com a query type, ou lang no vigente)", example = "Terms of use.")
        String content,
        @Schema(description = "Rascunho da linha (com type em rota MASTER; omitido no vigente)", example = "true")
        Boolean draft,
        @Schema(description = "Linhas do id (presente só sem a query type, ou sem lang no vigente)")
        List<TermContentResponse> contents) {

    public static TermResponse line(final TermDetail detail, final boolean includeDraft) {
        return new TermResponse(
                detail.termId(), detail.type(), detail.content(), includeDraft ? detail.draft() : null, null);
    }

    public static TermResponse of(final List<TermDetail> lines, final TermDetail.Type lang, final boolean includeDraft) {
        if (lang != null) {
            return line(lines.get(0), includeDraft);
        }
        return new TermResponse(
                lines.get(0).termId(),
                null,
                null,
                null,
                lines.stream().map(line -> TermContentResponse.of(line, includeDraft)).toList());
    }

}
