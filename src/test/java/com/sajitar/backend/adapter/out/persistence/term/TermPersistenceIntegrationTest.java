package com.sajitar.backend.adapter.out.persistence.term;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.sajitar.backend.domain.model.term.Term;

@SpringBootTest
@Transactional
@DisplayName("Term (gatilho de publicação no Postgres)")
class TermPersistenceIntegrationTest {

    @Autowired
    private TermJpaRepository jpa;

    @Test
    @DisplayName("draft false sem EN, PT e ES é recusado pelo gatilho")
    void triggerRejectsIncompletePublication() {
        final var id = UUID.fromString("019c5000-a111-7000-8000-111111111111");
        final var incomplete = TermJpaEntity.builder()
                .id(id)
                .type(Term.Type.EN)
                .content("Only EN")
                .draft(false)
                .build();

        assertThatThrownBy(() -> jpa.saveAndFlush(incomplete))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("draft false com EN, PT e ES é aceito pelo gatilho")
    void triggerAcceptsCompletePublication() {
        final var id = UUID.fromString("019c5000-b111-7000-8000-222222222222");
        jpa.saveAndFlush(TermJpaEntity.builder().id(id).type(Term.Type.EN).content("EN").draft(true).build());
        jpa.saveAndFlush(TermJpaEntity.builder().id(id).type(Term.Type.PT).content("PT").draft(true).build());
        jpa.saveAndFlush(TermJpaEntity.builder().id(id).type(Term.Type.ES).content("ES").draft(false).build());

        assertThat(jpa.findById(new TermId(id, Term.Type.ES)).orElseThrow().isDraft()).isFalse();
    }

}
