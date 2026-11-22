package com.sajitar.backend.adapter.out.persistence.term;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sajitar.backend.domain.model.term.Term;
import com.sajitar.backend.domain.model.term.TermDetail;

@DisplayName("TermPersistenceMapper")
class TermPersistenceMapperTest {

    @Test
    @DisplayName("toTermEntity e toDomain preservam o id")
    void roundTripTerm() {
        final var domain = new Term(UUID.fromString("019c4000-a111-7000-8000-111111111111"));

        final var entity = TermPersistenceMapper.toTermEntity(domain);
        assertThat(entity.getId()).isEqualTo(domain.id());

        final var back = TermPersistenceMapper.toDomain(entity);
        assertThat(back.id()).isEqualTo(domain.id());
    }

    @Test
    @DisplayName("toDetailEntity e toDomain preservam os campos da linha")
    void roundTripDetail() {
        final var domain = new TermDetail(
                UUID.fromString("019c4000-a111-7000-8000-111111111111"),
                TermDetail.Type.PT,
                "Olá.",
                false);

        final var entity = TermPersistenceMapper.toDetailEntity(domain);
        assertThat(entity.getTermId()).isEqualTo(domain.termId());
        assertThat(entity.getType()).isEqualTo(TermDetail.Type.PT);
        assertThat(entity.getContent()).isEqualTo("Olá.");
        assertThat(entity.isDraft()).isFalse();

        final var back = TermPersistenceMapper.toDomain(entity);
        assertThat(back.termId()).isEqualTo(domain.termId());
        assertThat(back.type()).isEqualTo(domain.type());
        assertThat(back.content()).isEqualTo(domain.content());
        assertThat(back.draft()).isEqualTo(domain.draft());
    }

}
