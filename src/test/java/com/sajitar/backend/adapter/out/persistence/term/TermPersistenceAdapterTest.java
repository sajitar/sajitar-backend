package com.sajitar.backend.adapter.out.persistence.term;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sajitar.backend.domain.model.term.Term;
import com.sajitar.backend.domain.model.term.TermDetail;
import com.sajitar.backend.domain.port.term.TermPageCriteria;

import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
@DisplayName("TermPersistenceAdapter")
class TermPersistenceAdapterTest {

    private static final UUID TERM_ID = UUID.fromString("019c4000-a111-7000-8000-111111111111");

    private static final UUID CURSOR = UUID.fromString("019c4000-a112-7000-8000-222222222222");

    @Mock
    private TermJpaRepository terms;

    @Mock
    private TermDetailJpaRepository details;

    @Mock
    private EntityManager entityManager;

    private TermPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new TermPersistenceAdapter(terms, details, entityManager);
    }

    @Test
    @DisplayName("save grava term antes de term_detail e as leituras delegam ao detalhe")
    void delegatesCrud() {
        final var term = new Term(TERM_ID);
        final var detail = new TermDetail(TERM_ID, TermDetail.Type.EN, "Hello.", true);
        final var termEntity = TermPersistenceMapper.toTermEntity(term);
        final var detailEntity = TermPersistenceMapper.toDetailEntity(detail);
        when(terms.saveAndFlush(any(TermJpaEntity.class))).thenReturn(termEntity);
        when(details.save(any(TermDetailJpaEntity.class))).thenReturn(detailEntity);
        when(details.findLinesById(TERM_ID)).thenReturn(List.of(detailEntity));
        when(details.findById(new TermId(TERM_ID, TermDetail.Type.EN))).thenReturn(Optional.of(detailEntity));

        assertThat(adapter.save(term, detail).content()).isEqualTo("Hello.");
        assertThat(adapter.findByIdAll(TERM_ID)).containsExactly(detail);
        assertThat(adapter.findByIdAndType(TERM_ID, TermDetail.Type.EN)).contains(detail);

        final InOrder order = Mockito.inOrder(terms, details);
        order.verify(terms).saveAndFlush(termEntity);
        order.verify(details).save(detailEntity);

        when(entityManager.contains(detailEntity)).thenReturn(true);
        adapter.deleteById(TERM_ID);
        verify(entityManager).detach(detailEntity);
        verify(terms).deleteById(TERM_ID);
        verify(details, never()).delete(any());
    }

    @Test
    @DisplayName("deleteById apaga term e deixa linha fora do contexto como está")
    void deleteDetachesOnlyManagedLines() {
        final var detailEntity = TermDetailJpaEntity.builder()
                .termId(TERM_ID)
                .type(TermDetail.Type.EN)
                .content("Hello.")
                .draft(true)
                .build();
        when(details.findLinesById(TERM_ID)).thenReturn(List.of(detailEntity));
        when(entityManager.contains(detailEntity)).thenReturn(false);

        adapter.deleteById(TERM_ID);

        verify(entityManager, never()).detach(any());
        verify(terms).deleteById(TERM_ID);
    }

    @Test
    @DisplayName("findPageIds sem type: ASC e DESC com e sem cursor")
    void findPageIdsWithoutTypeFilter() {
        when(details.findPageIds(10)).thenReturn(List.of(TERM_ID));
        when(details.findPageIdsAfter(CURSOR, 2)).thenReturn(List.of());
        when(details.findPageIdsDescending(10)).thenReturn(List.of());
        when(details.findPageIdsDescendingAfter(CURSOR, 2)).thenReturn(List.of());

        assertThat(adapter.findPageIds(new TermPageCriteria(null, null, 10, false))).containsExactly(TERM_ID);
        assertThat(adapter.findPageIds(new TermPageCriteria(null, CURSOR, 2, false))).isEmpty();
        assertThat(adapter.findPageIds(new TermPageCriteria(null, null, 10, true))).isEmpty();
        assertThat(adapter.findPageIds(new TermPageCriteria(null, CURSOR, 2, true))).isEmpty();
        verify(details).findPageIds(10);
        verify(details).findPageIdsAfter(CURSOR, 2);
        verify(details).findPageIdsDescending(10);
        verify(details).findPageIdsDescendingAfter(CURSOR, 2);
    }

    @Test
    @DisplayName("findPageIds com type: ASC e DESC com e sem cursor")
    void findPageIdsWithTypeFilter() {
        when(details.findPageIdsByType(TermDetail.Type.EN.name(), 10)).thenReturn(List.of());
        when(details.findPageIdsByTypeAfter(TermDetail.Type.EN.name(), CURSOR, 2)).thenReturn(List.of());
        when(details.findPageIdsByTypeDescending(TermDetail.Type.PT.name(), 10)).thenReturn(List.of());
        when(details.findPageIdsByTypeDescendingAfter(TermDetail.Type.ES.name(), CURSOR, 2)).thenReturn(List.of());

        assertThat(adapter.findPageIds(new TermPageCriteria(TermDetail.Type.EN, null, 10, false))).isEmpty();
        assertThat(adapter.findPageIds(new TermPageCriteria(TermDetail.Type.EN, CURSOR, 2, false))).isEmpty();
        assertThat(adapter.findPageIds(new TermPageCriteria(TermDetail.Type.PT, null, 10, true))).isEmpty();
        assertThat(adapter.findPageIds(new TermPageCriteria(TermDetail.Type.ES, CURSOR, 2, true))).isEmpty();
        verify(details).findPageIdsByType(TermDetail.Type.EN.name(), 10);
        verify(details).findPageIdsByTypeAfter(TermDetail.Type.EN.name(), CURSOR, 2);
        verify(details).findPageIdsByTypeDescending(TermDetail.Type.PT.name(), 10);
        verify(details).findPageIdsByTypeDescendingAfter(TermDetail.Type.ES.name(), CURSOR, 2);
    }

    @Test
    @DisplayName("countAfterCursor: zero sem cursor, ASC/DESC com e sem type")
    void countAfterCursorDirections() {
        when(details.countIdsAfter(CURSOR)).thenReturn(2L);
        when(details.countIdsBefore(CURSOR)).thenReturn(1L);
        when(details.countIdsByTypeAfter(TermDetail.Type.EN.name(), CURSOR)).thenReturn(3L);
        when(details.countIdsByTypeBefore(TermDetail.Type.PT.name(), CURSOR)).thenReturn(4L);

        assertThat(adapter.countAfterCursor(new TermPageCriteria(null, null, 10, false))).isZero();
        assertThat(adapter.countAfterCursor(new TermPageCriteria(null, CURSOR, 10, false))).isEqualTo(2L);
        assertThat(adapter.countAfterCursor(new TermPageCriteria(null, CURSOR, 10, true))).isEqualTo(1L);
        assertThat(adapter.countAfterCursor(new TermPageCriteria(TermDetail.Type.EN, CURSOR, 10, false))).isEqualTo(3L);
        assertThat(adapter.countAfterCursor(new TermPageCriteria(TermDetail.Type.PT, CURSOR, 10, true))).isEqualTo(4L);
        verify(details).countIdsAfter(CURSOR);
        verify(details).countIdsBefore(CURSOR);
        verify(details).countIdsByTypeAfter(TermDetail.Type.EN.name(), CURSOR);
        verify(details).countIdsByTypeBefore(TermDetail.Type.PT.name(), CURSOR);
    }

    @Test
    @DisplayName("findCurrentId devolve o primeiro id ou vazio")
    void findCurrentId() {
        when(details.findCurrentIds(1)).thenReturn(List.of(TERM_ID)).thenReturn(List.of());

        assertThat(adapter.findCurrentId()).contains(TERM_ID);
        assertThat(adapter.findCurrentId()).isEmpty();
    }

}
