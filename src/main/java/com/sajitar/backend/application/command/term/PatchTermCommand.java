package com.sajitar.backend.application.command.term;

import java.util.UUID;

import com.sajitar.backend.application.command.PatchValue;
import com.sajitar.backend.domain.model.term.Term;

import jakarta.validation.constraints.NotNull;

public record PatchTermCommand(
        @NotNull(message = "{validation.not-null}") UUID id,
        @NotNull(message = "{validation.not-null}") Term.Type type,
        PatchValue<String> content,
        PatchValue<Boolean> draft) {

    public PatchTermCommand {
        content = content == null ? PatchValue.absent() : content;
        draft = draft == null ? PatchValue.absent() : draft;
    }

}
