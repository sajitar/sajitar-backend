package com.sajitar.backend.application.usecase.note;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sajitar.backend.application.query.note.ListNotesQuery;
import com.sajitar.backend.domain.model.note.Note;
import com.sajitar.backend.domain.port.note.NotePageCriteria;
import com.sajitar.backend.domain.port.note.NoteRepository;
import com.sajitar.backend.domain.validation.Limit;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListNotesUseCase")
class ListNotesUseCaseTest {

    @Mock
    private NoteRepository notes;

    private ListNotesUseCase useCase;

    @BeforeAll
    static void configureValidation() {
        Limit.LimitValidator.configure(100);
    }

    @BeforeEach
    void setUp() {
        useCase = new ListNotesUseCase(notes, NoteUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Primeira página: conta apenas following e preceding permanece 0")
    void firstPageCountsOnlyFollowing() {
        final var first = NoteUseCaseFixture.persistedPublic();
        final var last = NoteUseCaseFixture.persistedProtected();
        when(notes.findPage(any(NotePageCriteria.class))).thenReturn(List.of(first, last));
        when(notes.countAfterCursor(any(NotePageCriteria.class))).thenReturn(12L);

        final var page = useCase.execute(new ListNotesQuery(NoteUseCaseFixture.PROFILE_ID, null, null, 10, false));

        assertThat(page.content()).containsExactly(first, last);
        assertThat(page.precedingElements()).isZero();
        assertThat(page.followingElements()).isEqualTo(12L);
        assertThat(page.reverse()).isFalse();
        verify(notes).findPage(new NotePageCriteria(NoteUseCaseFixture.PROFILE_ID, null, null, 10, false));
        verify(notes).countAfterCursor(
                new NotePageCriteria(NoteUseCaseFixture.PROFILE_ID, null, last.id(), 10, false));
    }

    @Test
    @DisplayName("Primeira página com filtro de type preserva o type no cursor")
    void firstPageWithTypeFilterPreservesTypeOnCursor() {
        final var first = NoteUseCaseFixture.persistedPublic();
        when(notes.findPage(any(NotePageCriteria.class))).thenReturn(List.of(first));
        when(notes.countAfterCursor(any(NotePageCriteria.class))).thenReturn(1L);

        final var page = useCase.execute(
                new ListNotesQuery(NoteUseCaseFixture.PROFILE_ID, Note.Type.PUBLIC, null, 1, false));

        assertThat(page.followingElements()).isEqualTo(1L);
        verify(notes).findPage(new NotePageCriteria(
                NoteUseCaseFixture.PROFILE_ID, Note.Type.PUBLIC, null, 1, false));
        verify(notes).countAfterCursor(new NotePageCriteria(
                NoteUseCaseFixture.PROFILE_ID, Note.Type.PUBLIC, first.id(), 1, false));
    }

    @Test
    @DisplayName("Continuação de cursor: calcula preceding e following")
    void continuationCountsPrecedingAndFollowing() {
        final var first = NoteUseCaseFixture.persistedProtected();
        final var last = NoteUseCaseFixture.persistedPublic();
        when(notes.findPage(any(NotePageCriteria.class))).thenReturn(List.of(first, last));
        when(notes.countAfterCursor(any(NotePageCriteria.class))).thenReturn(4L, 7L);

        final var page = useCase.execute(new ListNotesQuery(
                NoteUseCaseFixture.PROFILE_ID, null, NoteUseCaseFixture.ID, 2, false));

        assertThat(page.followingElements()).isEqualTo(4L);
        assertThat(page.precedingElements()).isEqualTo(7L);
        assertThat(page.reverse()).isFalse();
        verify(notes).findPage(new NotePageCriteria(
                NoteUseCaseFixture.PROFILE_ID, null, NoteUseCaseFixture.ID, 2, false));
        verify(notes, times(2)).countAfterCursor(any(NotePageCriteria.class));
    }

    @Test
    @DisplayName("Página vazia reverse ecoa o critério e não conta cursores")
    void emptyPageDoesNotCount() {
        when(notes.findPage(any(NotePageCriteria.class))).thenReturn(List.of());

        final var page = useCase.execute(new ListNotesQuery(
                NoteUseCaseFixture.PROFILE_ID, null, NoteUseCaseFixture.ID, 2, true));

        assertThat(page.isEmpty()).isTrue();
        assertThat(page.reverse()).isTrue();
        verify(notes).findPage(new NotePageCriteria(
                NoteUseCaseFixture.PROFILE_ID, null, NoteUseCaseFixture.ID, 2, true));
        verify(notes, never()).countAfterCursor(any());
    }

    @Test
    @DisplayName("Primeira página reverse: following na direção descendente")
    void firstPageReverseCountsFollowingDescending() {
        final var first = NoteUseCaseFixture.persistedProtected();
        final var last = NoteUseCaseFixture.persistedPublic();
        when(notes.findPage(any(NotePageCriteria.class))).thenReturn(List.of(first, last));
        when(notes.countAfterCursor(any(NotePageCriteria.class))).thenReturn(3L);

        final var page = useCase.execute(new ListNotesQuery(NoteUseCaseFixture.PROFILE_ID, null, null, 10, true));

        assertThat(page.reverse()).isTrue();
        assertThat(page.precedingElements()).isZero();
        assertThat(page.followingElements()).isEqualTo(3L);
        verify(notes).findPage(new NotePageCriteria(NoteUseCaseFixture.PROFILE_ID, null, null, 10, true));
        verify(notes).countAfterCursor(
                new NotePageCriteria(NoteUseCaseFixture.PROFILE_ID, null, last.id(), 10, true));
    }

    @Test
    @DisplayName("Continuação reverse: preceding na direção oposta")
    void continuationReverseCountsPrecedingOpposite() {
        final var first = NoteUseCaseFixture.persistedProtected();
        final var last = NoteUseCaseFixture.persistedPublic();
        when(notes.findPage(any(NotePageCriteria.class))).thenReturn(List.of(first, last));
        when(notes.countAfterCursor(any(NotePageCriteria.class))).thenReturn(0L, 1L);

        final var cursor = NoteUseCaseFixture.persistedProtected().id();
        final var page = useCase.execute(new ListNotesQuery(
                NoteUseCaseFixture.PROFILE_ID, null, cursor, 2, true));

        assertThat(page.followingElements()).isZero();
        assertThat(page.precedingElements()).isEqualTo(1L);
        assertThat(page.reverse()).isTrue();
        verify(notes).findPage(new NotePageCriteria(
                NoteUseCaseFixture.PROFILE_ID, null, cursor, 2, true));
        verify(notes).countAfterCursor(
                new NotePageCriteria(NoteUseCaseFixture.PROFILE_ID, null, last.id(), 2, true));
        verify(notes).countAfterCursor(
                new NotePageCriteria(NoteUseCaseFixture.PROFILE_ID, null, first.id(), 2, false));
    }

    @Test
    @DisplayName("profileId nulo: não consulta o repositório")
    void rejectsNullProfileId() {
        final var thrown = catchThrowable(() -> useCase.execute(new ListNotesQuery(null, null, null, 10, false)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotNull.class);
        verify(notes, never()).findPage(any());
        verify(notes, never()).countAfterCursor(any());
    }

    @Test
    @DisplayName("reverse nulo: não consulta o repositório")
    void rejectsNullReverse() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new ListNotesQuery(NoteUseCaseFixture.PROFILE_ID, null, null, 10, null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotNull.class);
        verify(notes, never()).findPage(any());
    }

    @Test
    @DisplayName("limit inválido: não consulta o repositório")
    void rejectsInvalidLimit() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new ListNotesQuery(NoteUseCaseFixture.PROFILE_ID, null, null, 0, false)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(notes, never()).findPage(any());
    }

    @Test
    @DisplayName("limit nulo: não consulta o repositório")
    void rejectsNullLimit() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new ListNotesQuery(NoteUseCaseFixture.PROFILE_ID, null, null, null, false)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(notes, never()).findPage(any());
    }

    @Test
    @DisplayName("limit acima do máximo: não consulta o repositório")
    void rejectsLimitAboveMax() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new ListNotesQuery(NoteUseCaseFixture.PROFILE_ID, null, null, 101, false)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(notes, never()).findPage(any());
    }

}
