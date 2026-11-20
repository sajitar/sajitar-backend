package com.sajitar.backend.adapter.out.persistence.term;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sajitar.backend.domain.model.term.Term;
import com.sajitar.backend.domain.port.term.TermPageCriteria;

@ExtendWith(MockitoExtension.class)
@DisplayName("TermPersistenceAdapter")
class TermPersistenceAdapterTest {

    private static final UUID TERM_ID = UUID.fromString("019c4000-a111-7000-8000-111111111111");

    private static final UUID CURSOR = UUID.fromString("019c4000-a112-7000-8000-222222222222");

    @Mock
    private TermJpaRepository jpa;

    private TermPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new TermPersistenceAdapter(jpa);
    }

    @Test
    @DisplayName("save, findByIdAll, findByIdAndType e deleteById delegam ao JPA")
    void delegatesCrud() {
        final var domain = new Term(TERM_ID, Term.Type.EN, "Hello.", true);
        final var entity = TermPersistenceMapper.toEntity(domain);
        when(jpa.save(any(TermJpaEntity.class))).thenReturn(entity);
        when(jpa.findLinesById(TERM_ID)).thenReturn(List.of(entity));
        when(jpa.findById(new TermId(TERM_ID, Term.Type.EN))).thenReturn(Optional.of(entity));

        assertThat(adapter.save(domain).content()).isEqualTo("Hello.");
        assertThat(adapter.findByIdAll(TERM_ID)).containsExactly(domain);
        assertThat(adapter.findByIdAndType(TERM_ID, Term.Type.EN)).contains(domain);

        adapter.deleteById(TERM_ID);
        verify(jpa).deleteAll(List.of(entity));
    }

    @Test
    @DisplayName("findPageIds sem type: ASC e DESC com e sem cursor")
    void findPageIdsWithoutTypeFilter() {
        when(jpa.findPageIds(10)).thenReturn(List.of(TERM_ID));
        when(jpa.findPageIdsAfter(CURSOR, 2)).thenReturn(List.of());
        when(jpa.findPageIdsDescending(10)).thenReturn(List.of());
        when(jpa.findPageIdsDescendingAfter(CURSOR, 2)).thenReturn(List.of());

        assertThat(adapter.findPageIds(new TermPageCriteria(null, null, 10, false))).containsExactly(TERM_ID);
        assertThat(adapter.findPageIds(new TermPageCriteria(null, CURSOR, 2, false))).isEmpty();
        assertThat(adapter.findPageIds(new TermPageCriteria(null, null, 10, true))).isEmpty();
        assertThat(adapter.findPageIds(new TermPageCriteria(null, CURSOR, 2, true))).isEmpty();
        verify(jpa).findPageIds(10);
        verify(jpa).findPageIdsAfter(CURSOR, 2);
        verify(jpa).findPageIdsDescending(10);
        verify(jpa).findPageIdsDescendingAfter(CURSOR, 2);
    }

    @Test
    @DisplayName("findPageIds com type: ASC e DESC com e sem cursor")
    void findPageIdsWithTypeFilter() {
        when(jpa.findPageIdsByType(Term.Type.EN.name(), 10)).thenReturn(List.of());
        when(jpa.findPageIdsByTypeAfter(Term.Type.EN.name(), CURSOR, 2)).thenReturn(List.of());
        when(jpa.findPageIdsByTypeDescending(Term.Type.PT.name(), 10)).thenReturn(List.of());
        when(jpa.findPageIdsByTypeDescendingAfter(Term.Type.ES.name(), CURSOR, 2)).thenReturn(List.of());

        assertThat(adapter.findPageIds(new TermPageCriteria(Term.Type.EN, null, 10, false))).isEmpty();
        assertThat(adapter.findPageIds(new TermPageCriteria(Term.Type.EN, CURSOR, 2, false))).isEmpty();
        assertThat(adapter.findPageIds(new TermPageCriteria(Term.Type.PT, null, 10, true))).isEmpty();
        assertThat(adapter.findPageIds(new TermPageCriteria(Term.Type.ES, CURSOR, 2, true))).isEmpty();
        verify(jpa).findPageIdsByType(Term.Type.EN.name(), 10);
        verify(jpa).findPageIdsByTypeAfter(Term.Type.EN.name(), CURSOR, 2);
        verify(jpa).findPageIdsByTypeDescending(Term.Type.PT.name(), 10);
        verify(jpa).findPageIdsByTypeDescendingAfter(Term.Type.ES.name(), CURSOR, 2);
    }

    @Test
    @DisplayName("countAfterCursor: zero sem cursor, ASC/DESC com e sem type")
    void countAfterCursorDirections() {
        when(jpa.countIdsAfter(CURSOR)).thenReturn(2L);
        when(jpa.countIdsBefore(CURSOR)).thenReturn(1L);
        when(jpa.countIdsByTypeAfter(Term.Type.EN.name(), CURSOR)).thenReturn(3L);
        when(jpa.countIdsByTypeBefore(Term.Type.PT.name(), CURSOR)).thenReturn(4L);

        assertThat(adapter.countAfterCursor(new TermPageCriteria(null, null, 10, false))).isZero();
        assertThat(adapter.countAfterCursor(new TermPageCriteria(null, CURSOR, 10, false))).isEqualTo(2L);
        assertThat(adapter.countAfterCursor(new TermPageCriteria(null, CURSOR, 10, true))).isEqualTo(1L);
        assertThat(adapter.countAfterCursor(new TermPageCriteria(Term.Type.EN, CURSOR, 10, false))).isEqualTo(3L);
        assertThat(adapter.countAfterCursor(new TermPageCriteria(Term.Type.PT, CURSOR, 10, true))).isEqualTo(4L);
        verify(jpa).countIdsAfter(CURSOR);
        verify(jpa).countIdsBefore(CURSOR);
        verify(jpa).countIdsByTypeAfter(Term.Type.EN.name(), CURSOR);
        verify(jpa).countIdsByTypeBefore(Term.Type.PT.name(), CURSOR);
    }

    @Test
    @DisplayName("findCurrentId devolve o primeiro id ou vazio")
    void findCurrentId() {
        when(jpa.findCurrentIds(1)).thenReturn(List.of(TERM_ID)).thenReturn(List.of());

        assertThat(adapter.findCurrentId()).contains(TERM_ID);
        assertThat(adapter.findCurrentId()).isEmpty();
    }

}
