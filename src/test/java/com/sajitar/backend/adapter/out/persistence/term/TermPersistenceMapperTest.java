package com.sajitar.backend.adapter.out.persistence.term;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sajitar.backend.domain.model.term.Term;

@DisplayName("TermPersistenceMapper")
class TermPersistenceMapperTest {

    @Test
    @DisplayName("toEntity e toDomain preservam os campos")
    void roundTrip() {
        final var domain = new Term(
                UUID.fromString("019c4000-a111-7000-8000-111111111111"),
                Term.Type.PT,
                "Olá.",
                false);

        final var entity = TermPersistenceMapper.toEntity(domain);
        assertThat(entity.getId()).isEqualTo(domain.id());
        assertThat(entity.getType()).isEqualTo(Term.Type.PT);
        assertThat(entity.getContent()).isEqualTo("Olá.");
        assertThat(entity.isDraft()).isFalse();

        final var back = TermPersistenceMapper.toDomain(entity);
        assertThat(back.id()).isEqualTo(domain.id());
        assertThat(back.type()).isEqualTo(domain.type());
        assertThat(back.content()).isEqualTo(domain.content());
        assertThat(back.draft()).isEqualTo(domain.draft());
    }

}
