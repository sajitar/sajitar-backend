package com.sajitar.backend.adapter.in.web.contract.term;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sajitar.backend.domain.model.term.Term;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "Term",
        description = "Representação de um Term. Com a query lang: id, type, content e draft. Sem lang: id e contents.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TermResponse(
        @Schema(description = "Identificador do Term", example = "019c4000-a111-7000-8000-111111111111")
        UUID id,
        @Schema(description = "Língua da linha (presente só com a query lang)", example = "EN")
        Term.Type type,
        @Schema(description = "Texto da linha (presente só com a query lang)", example = "Terms of use.")
        String content,
        @Schema(description = "Rascunho da linha (só com lang em rota MASTER; omitido no vigente)", example = "true")
        Boolean draft,
        @Schema(description = "Linhas do id (presente só sem a query lang)")
        List<TermContentResponse> contents) {

    public static TermResponse line(final Term term, final boolean includeDraft) {
        return new TermResponse(term.id(), term.type(), term.content(), includeDraft ? term.draft() : null, null);
    }

    public static TermResponse of(final List<Term> lines, final Term.Type lang, final boolean includeDraft) {
        if (lang != null) {
            return line(lines.get(0), includeDraft);
        }
        return new TermResponse(
                lines.get(0).id(),
                null,
                null,
                null,
                lines.stream().map(line -> TermContentResponse.of(line, includeDraft)).toList());
    }

}
