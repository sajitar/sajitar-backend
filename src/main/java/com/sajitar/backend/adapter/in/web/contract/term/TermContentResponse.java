package com.sajitar.backend.adapter.in.web.contract.term;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sajitar.backend.domain.model.term.Term;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TermContent", description = "Linha de um Term: type, content e draft. O campo draft é omitido no vigente.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TermContentResponse(
        @Schema(description = "Língua da linha", example = "EN")
        Term.Type type,
        @Schema(description = "Texto da linha", example = "Terms of use.")
        String content,
        @Schema(description = "Rascunho da linha; omitido no vigente", example = "true")
        Boolean draft) {

    static TermContentResponse of(final Term term, final boolean includeDraft) {
        return new TermContentResponse(term.type(), term.content(), includeDraft ? term.draft() : null);
    }

}
