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

import com.sajitar.backend.domain.model.term.TermDetail;

import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
@DisplayName("Term (gatilho de publicação no Postgres)")
class TermPersistenceIntegrationTest {

    @Autowired
    private TermJpaRepository terms;

    @Autowired
    private TermDetailJpaRepository details;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("draft false sem EN, PT e ES é recusado pelo gatilho")
    void triggerRejectsIncompletePublication() {
        final var id = UUID.fromString("019c5000-a111-7000-8000-111111111111");
        terms.saveAndFlush(TermJpaEntity.builder().id(id).build());
        final var incomplete = line(id, TermDetail.Type.EN, false);

        assertThatThrownBy(() -> details.saveAndFlush(incomplete))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("draft false com EN, PT e ES é aceito pelo gatilho")
    void triggerAcceptsCompletePublication() {
        final var id = UUID.fromString("019c5000-b111-7000-8000-222222222222");
        terms.saveAndFlush(TermJpaEntity.builder().id(id).build());
        details.saveAndFlush(line(id, TermDetail.Type.EN, true));
        details.saveAndFlush(line(id, TermDetail.Type.PT, true));
        details.saveAndFlush(line(id, TermDetail.Type.ES, false));

        assertThat(details.findById(new TermId(id, TermDetail.Type.ES)).orElseThrow().isDraft()).isFalse();
    }

    @Test
    @DisplayName("apagar term esvazia term_detail pela cascata")
    void deletingTermRemovesDetails() {
        final var id = UUID.fromString("019c5000-c111-7000-8000-333333333333");
        terms.saveAndFlush(TermJpaEntity.builder().id(id).build());
        details.saveAndFlush(line(id, TermDetail.Type.EN, true));
        entityManager.clear();

        terms.deleteById(id);
        entityManager.flush();

        assertThat(terms.findById(id)).isEmpty();
        assertThat(details.findLinesById(id)).isEmpty();
    }

    private static TermDetailJpaEntity line(final UUID id, final TermDetail.Type type, final boolean draft) {
        return TermDetailJpaEntity.builder()
                .termId(id)
                .type(type)
                .content(type.name())
                .draft(draft)
                .build();
    }

}
