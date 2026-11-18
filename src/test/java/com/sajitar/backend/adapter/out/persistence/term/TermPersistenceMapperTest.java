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
                UUID.fromString("019c4000-a222-7000-8000-222222222222"),
                "Termo vigente.");

        final var entity = TermPersistenceMapper.toEntity(domain);
        assertThat(entity.getId()).isEqualTo(domain.id());
        assertThat(entity.getContent()).isEqualTo(domain.content());

        final var back = TermPersistenceMapper.toDomain(entity);
        assertThat(back.id()).isEqualTo(domain.id());
        assertThat(back.content()).isEqualTo(domain.content());
    }

}
