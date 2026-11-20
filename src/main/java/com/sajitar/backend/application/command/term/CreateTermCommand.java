package com.sajitar.backend.application.command.term;

import com.sajitar.backend.domain.model.term.Term;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTermCommand(
        @NotNull(message = "{validation.not-null}") Term.Type type,
        @NotBlank(message = "{validation.not-blank}") String content) {

}
