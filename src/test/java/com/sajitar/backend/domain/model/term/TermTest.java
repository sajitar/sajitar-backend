package com.sajitar.backend.domain.model.term;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Term (agregado)")
class TermTest {

    @Test
    @DisplayName("create gera id e preserva content")
    void createGeneratesId() {
        final var created = Term.create("Texto do termo vigente.");

        assertThat(created.id()).isNotNull();
        assertThat(created.content()).isEqualTo("Texto do termo vigente.");
    }

    @Test
    @DisplayName("withContent copia o id")
    void withContentCopiesId() {
        final var original = Term.create("Texto do termo vigente.");
        final var updated = original.withContent("Outro texto.");

        assertThat(updated.id()).isEqualTo(original.id());
        assertThat(updated.content()).isEqualTo("Outro texto.");
    }

    @Test
    @DisplayName("equals considera apenas o id e rejeita outros tipos")
    void equalsByIdOnly() {
        final var id = UUID.randomUUID();
        final var a = new Term(id, "a");
        final var b = new Term(id, "b");
        final var c = new Term(UUID.randomUUID(), "a");

        assertThat(a).isEqualTo(b).isNotEqualTo(c).isNotEqualTo("nao-e-term").isNotEqualTo(null);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

}
