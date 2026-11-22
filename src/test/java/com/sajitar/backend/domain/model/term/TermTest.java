package com.sajitar.backend.domain.model.term;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Term (agregado)")
class TermTest {

    @Test
    @DisplayName("create gera id UUIDv7")
    void createGeneratesId() {
        final var created = Term.create();

        assertThat(created.id()).isNotNull();
        assertThat(created.id().version()).isEqualTo(7);
    }

}
