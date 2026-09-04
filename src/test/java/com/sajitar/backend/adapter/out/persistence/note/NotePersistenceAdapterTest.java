package com.sajitar.backend.adapter.out.persistence.note;

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

import com.sajitar.backend.domain.model.note.Note;
import com.sajitar.backend.domain.port.note.NotePageCriteria;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotePersistenceAdapter")
class NotePersistenceAdapterTest {

    @Mock
    private NoteJpaRepository jpa;

    private NotePersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new NotePersistenceAdapter(jpa);
    }

    @Test
    @DisplayName("save, find e delete delegam ao JPA")
    void delegatesCrud() {
        final var domain = new Note(
                UUID.fromString("019c3000-a111-7000-8000-111111111111"),
                UUID.fromString("01989bad-6161-7000-0ae9-f440b10578ec"),
                Note.Type.PUBLIC,
                "Alice public one");
        final var entity = NotePersistenceMapper.toEntity(domain);
        when(jpa.save(any(NoteJpaEntity.class))).thenReturn(entity);
        when(jpa.findById(domain.id())).thenReturn(Optional.of(entity));

        assertThat(adapter.save(domain).id()).isEqualTo(domain.id());
        assertThat(adapter.findById(domain.id())).contains(domain);
        adapter.deleteById(domain.id());
        verify(jpa).deleteById(domain.id());
    }

    @Test
    @DisplayName("findPage sem type: ASC e DESC com e sem cursor")
    void findPageWithoutTypeFilter() {
        final var profileId = UUID.fromString("01989bad-6161-7000-0ae9-f440b10578ec");
        final var lastSeenId = UUID.fromString("019c3000-a111-7000-8000-111111111111");
        when(jpa.findPageByProfileId(profileId, 10)).thenReturn(List.of());
        when(jpa.findPageByProfileIdAfter(profileId, lastSeenId, 2)).thenReturn(List.of());
        when(jpa.findPageByProfileIdDescending(profileId, 10)).thenReturn(List.of());
        when(jpa.findPageByProfileIdDescendingAfter(profileId, lastSeenId, 2)).thenReturn(List.of());

        assertThat(adapter.findPage(new NotePageCriteria(profileId, null, null, 10, false))).isEmpty();
        assertThat(adapter.findPage(new NotePageCriteria(profileId, null, lastSeenId, 2, false))).isEmpty();
        assertThat(adapter.findPage(new NotePageCriteria(profileId, null, null, 10, true))).isEmpty();
        assertThat(adapter.findPage(new NotePageCriteria(profileId, null, lastSeenId, 2, true))).isEmpty();
        verify(jpa).findPageByProfileId(profileId, 10);
        verify(jpa).findPageByProfileIdAfter(profileId, lastSeenId, 2);
        verify(jpa).findPageByProfileIdDescending(profileId, 10);
        verify(jpa).findPageByProfileIdDescendingAfter(profileId, lastSeenId, 2);
    }

    @Test
    @DisplayName("findPage com type: ASC e DESC com e sem cursor")
    void findPageWithTypeFilter() {
        final var profileId = UUID.fromString("01989bad-6161-7000-0ae9-f440b10578ec");
        final var lastSeenId = UUID.fromString("019c3000-a111-7000-8000-111111111111");
        when(jpa.findPageByProfileIdAndType(profileId, (short) 0, 10)).thenReturn(List.of());
        when(jpa.findPageByProfileIdAndTypeAfter(profileId, (short) 0, lastSeenId, 2)).thenReturn(List.of());
        when(jpa.findPageByProfileIdAndTypeDescending(profileId, (short) 2, 10)).thenReturn(List.of());
        when(jpa.findPageByProfileIdAndTypeDescendingAfter(profileId, (short) 1, lastSeenId, 2)).thenReturn(List.of());

        assertThat(adapter.findPage(
                new NotePageCriteria(profileId, Note.Type.PUBLIC, null, 10, false))).isEmpty();
        assertThat(adapter.findPage(
                new NotePageCriteria(profileId, Note.Type.PUBLIC, lastSeenId, 2, false))).isEmpty();
        assertThat(adapter.findPage(
                new NotePageCriteria(profileId, Note.Type.PRIVATE, null, 10, true))).isEmpty();
        assertThat(adapter.findPage(
                new NotePageCriteria(profileId, Note.Type.PROTECTED, lastSeenId, 2, true))).isEmpty();
        verify(jpa).findPageByProfileIdAndType(profileId, (short) 0, 10);
        verify(jpa).findPageByProfileIdAndTypeAfter(profileId, (short) 0, lastSeenId, 2);
        verify(jpa).findPageByProfileIdAndTypeDescending(profileId, (short) 2, 10);
        verify(jpa).findPageByProfileIdAndTypeDescendingAfter(profileId, (short) 1, lastSeenId, 2);
    }

    @Test
    @DisplayName("countAfterCursor ASC, DESC, com type e sem cursor")
    void countAfterCursorDirections() {
        final var profileId = UUID.fromString("01989bad-6161-7000-0ae9-f440b10578ec");
        final var lastSeenId = UUID.fromString("019c3000-a111-7000-8000-111111111111");
        when(jpa.countByProfileIdAndIdAfter(profileId, lastSeenId)).thenReturn(2L);
        when(jpa.countByProfileIdAndIdBefore(profileId, lastSeenId)).thenReturn(1L);
        when(jpa.countByProfileIdAndTypeAndIdAfter(profileId, (short) 0, lastSeenId)).thenReturn(3L);
        when(jpa.countByProfileIdAndTypeAndIdBefore(profileId, (short) 1, lastSeenId)).thenReturn(4L);

        assertThat(adapter.countAfterCursor(new NotePageCriteria(profileId, null, null, 10, false))).isZero();
        assertThat(adapter.countAfterCursor(
                new NotePageCriteria(profileId, null, lastSeenId, 10, false))).isEqualTo(2L);
        assertThat(adapter.countAfterCursor(
                new NotePageCriteria(profileId, null, lastSeenId, 10, true))).isEqualTo(1L);
        assertThat(adapter.countAfterCursor(
                new NotePageCriteria(profileId, Note.Type.PUBLIC, lastSeenId, 10, false))).isEqualTo(3L);
        assertThat(adapter.countAfterCursor(
                new NotePageCriteria(profileId, Note.Type.PROTECTED, lastSeenId, 10, true))).isEqualTo(4L);
        verify(jpa).countByProfileIdAndIdAfter(profileId, lastSeenId);
        verify(jpa).countByProfileIdAndIdBefore(profileId, lastSeenId);
        verify(jpa).countByProfileIdAndTypeAndIdAfter(profileId, (short) 0, lastSeenId);
        verify(jpa).countByProfileIdAndTypeAndIdBefore(profileId, (short) 1, lastSeenId);
    }

}
