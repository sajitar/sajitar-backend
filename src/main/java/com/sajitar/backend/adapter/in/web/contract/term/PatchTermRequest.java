package com.sajitar.backend.adapter.in.web.contract.term;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sajitar.backend.adapter.in.web.PatchValueDeserializer;
import com.sajitar.backend.application.command.PatchValue;
import com.sajitar.backend.application.command.term.PatchTermCommand;
import com.sajitar.backend.domain.model.term.TermDetail;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.annotation.JsonDeserialize;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
        name = "PatchTermRequest",
        description = "Alteração da linha da query type. content e draft são opcionais. Omitir content mantém o texto. Omitir draft mantém o valor gravado. content nulo, vazio ou em branco retorna 400. draft null retorna 400. Corpo sem os dois retorna 400 nos dois campos. Um type no corpo é ignorado.")
public record PatchTermRequest(
        @Schema(description = "Texto da linha. Omitir para manter. Null, vazio ou em branco retorna 400.", example = "Terms of use.")
        @JsonDeserialize(using = PatchValueDeserializer.class)
        PatchValue<String> content,
        @Schema(description = "Rascunho. Omitir mantém o valor gravado. true reabre. false publica se o id tem EN, PT e ES. null retorna 400.", example = "false")
        @JsonDeserialize(using = PatchValueDeserializer.class)
        PatchValue<Boolean> draft) {

    public PatchTermCommand toCommand(final UUID id, final TermDetail.Type type) {
        return new PatchTermCommand(id, type, content, draft);
    }

}
