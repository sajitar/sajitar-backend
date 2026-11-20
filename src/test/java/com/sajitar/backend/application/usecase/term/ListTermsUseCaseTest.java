package com.sajitar.backend.application.usecase.term;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sajitar.backend.application.query.term.ListTermsQuery;
import com.sajitar.backend.domain.exception.ForbiddenTermException;
import com.sajitar.backend.domain.model.term.Term;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.port.term.TermPageCriteria;
import com.sajitar.backend.domain.port.term.TermRepository;
import com.sajitar.backend.domain.validation.Limit;

import jakarta.validation.ConstraintViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListTermsUseCase")
class ListTermsUseCaseTest {

    private static final UUID CURSOR = UUID.fromString("019c4000-c000-7000-8000-cccccccccccc");

    @Mock
    private TermRepository terms;

    @Mock
    private ProfileRepository profiles;

    private ListTermsUseCase useCase;

    @BeforeAll
    static void configureValidation() {
        Limit.LimitValidator.configure(100);
    }

    @BeforeEach
    void setUp() {
        useCase = new ListTermsUseCase(terms, profiles, TermUseCaseFixture.VALIDATOR);
    }

    private void withMaster() {
        when(profiles.findById(TermUseCaseFixture.MASTER_ID))
                .thenReturn(Optional.of(TermUseCaseFixture.masterProfile()));
    }

    @Test
    @DisplayName("Primeira página sem lang: contents de cada id, following contado e preceding zero")
    void firstPageNoLang() {
        withMaster();
        final var en1 = TermUseCaseFixture.draft(TermUseCaseFixture.TERM_ID, Term.Type.EN);
        final var pt1 = TermUseCaseFixture.draft(TermUseCaseFixture.TERM_ID, Term.Type.PT);
        final var en2 = TermUseCaseFixture.draft(TermUseCaseFixture.OTHER_ID, Term.Type.EN);
        when(terms.findPageIds(new TermPageCriteria(null, null, 10, false)))
                .thenReturn(List.of(TermUseCaseFixture.TERM_ID, TermUseCaseFixture.OTHER_ID));
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID)).thenReturn(List.of(en1, pt1));
        when(terms.findByIdAll(TermUseCaseFixture.OTHER_ID)).thenReturn(List.of(en2));
        when(terms.countAfterCursor(new TermPageCriteria(null, TermUseCaseFixture.OTHER_ID, 10, false)))
                .thenReturn(5L);

        final var page = useCase.execute(new ListTermsQuery(null, null, 10, false), TermUseCaseFixture.MASTER_ID);

        assertThat(page.content()).hasSize(2);
        assertThat(page.content().get(0)).containsExactly(en1, pt1);
        assertThat(page.content().get(1)).containsExactly(en2);
        assertThat(page.precedingElements()).isZero();
        assertThat(page.followingElements()).isEqualTo(5L);
        assertThat(page.reverse()).isFalse();
    }

    @Test
    @DisplayName("Primeira página com lang: cada id traz só a linha do type")
    void firstPageWithLang() {
        withMaster();
        final var en1 = TermUseCaseFixture.draft(TermUseCaseFixture.TERM_ID, Term.Type.EN);
        final var pt1 = TermUseCaseFixture.draft(TermUseCaseFixture.TERM_ID, Term.Type.PT);
        when(terms.findPageIds(new TermPageCriteria(Term.Type.EN, null, 10, false)))
                .thenReturn(List.of(TermUseCaseFixture.TERM_ID));
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID)).thenReturn(List.of(en1, pt1));
        when(terms.countAfterCursor(new TermPageCriteria(Term.Type.EN, TermUseCaseFixture.TERM_ID, 10, false)))
                .thenReturn(1L);

        final var page = useCase.execute(
                new ListTermsQuery(Term.Type.EN, null, 10, false), TermUseCaseFixture.MASTER_ID);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0)).containsExactly(en1);
        assertThat(page.followingElements()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Continuação de cursor conta preceding e following")
    void continuationCountsBoth() {
        withMaster();
        when(terms.findPageIds(new TermPageCriteria(null, CURSOR, 2, false)))
                .thenReturn(List.of(TermUseCaseFixture.TERM_ID, TermUseCaseFixture.OTHER_ID));
        when(terms.findByIdAll(any())).thenReturn(List.of());
        when(terms.countAfterCursor(any(TermPageCriteria.class))).thenReturn(4L, 7L);

        final var page = useCase.execute(new ListTermsQuery(null, CURSOR, 2, false), TermUseCaseFixture.MASTER_ID);

        assertThat(page.followingElements()).isEqualTo(4L);
        assertThat(page.precedingElements()).isEqualTo(7L);
        verify(terms).countAfterCursor(new TermPageCriteria(null, TermUseCaseFixture.OTHER_ID, 2, false));
        verify(terms).countAfterCursor(new TermPageCriteria(null, TermUseCaseFixture.TERM_ID, 2, true));
        verify(terms, times(2)).countAfterCursor(any(TermPageCriteria.class));
    }

    @Test
    @DisplayName("Continuação de cursor reverse conta preceding e following")
    void continuationReverseCountsBoth() {
        withMaster();
        when(terms.findPageIds(new TermPageCriteria(null, CURSOR, 2, true)))
                .thenReturn(List.of(TermUseCaseFixture.TERM_ID, TermUseCaseFixture.OTHER_ID));
        when(terms.findByIdAll(any())).thenReturn(List.of());
        when(terms.countAfterCursor(any(TermPageCriteria.class))).thenReturn(4L, 7L);

        final var page = useCase.execute(new ListTermsQuery(null, CURSOR, 2, true), TermUseCaseFixture.MASTER_ID);

        assertThat(page.followingElements()).isEqualTo(4L);
        assertThat(page.precedingElements()).isEqualTo(7L);
        verify(terms).countAfterCursor(new TermPageCriteria(null, TermUseCaseFixture.OTHER_ID, 2, true));
        verify(terms).countAfterCursor(new TermPageCriteria(null, TermUseCaseFixture.TERM_ID, 2, false));
        verify(terms, times(2)).countAfterCursor(any(TermPageCriteria.class));
        assertThat(page.reverse()).isTrue();
    }

    @Test
    @DisplayName("Página vazia reverse ecoa o critério e não conta")
    void emptyPage() {
        withMaster();
        when(terms.findPageIds(new TermPageCriteria(null, CURSOR, 2, true))).thenReturn(List.of());

        final var page = useCase.execute(new ListTermsQuery(null, CURSOR, 2, true), TermUseCaseFixture.MASTER_ID);

        assertThat(page.isEmpty()).isTrue();
        assertThat(page.reverse()).isTrue();
        verify(terms, never()).countAfterCursor(any());
        verify(terms, never()).findByIdAll(any());
    }

    @Test
    @DisplayName("Primeira página reverse conta following na direção descendente")
    void firstPageReverse() {
        withMaster();
        when(terms.findPageIds(new TermPageCriteria(null, null, 10, true)))
                .thenReturn(List.of(TermUseCaseFixture.TERM_ID));
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID))
                .thenReturn(List.of(TermUseCaseFixture.draft(TermUseCaseFixture.TERM_ID, Term.Type.EN)));
        when(terms.countAfterCursor(new TermPageCriteria(null, TermUseCaseFixture.TERM_ID, 10, true)))
                .thenReturn(3L);

        final var page = useCase.execute(new ListTermsQuery(null, null, 10, true), TermUseCaseFixture.MASTER_ID);

        assertThat(page.reverse()).isTrue();
        assertThat(page.precedingElements()).isZero();
        assertThat(page.followingElements()).isEqualTo(3L);
    }

    @Test
    @DisplayName("Perfil sem MASTER lança ForbiddenTermException e não pagina")
    void forbiddenWhenNotMaster() {
        when(profiles.findById(TermUseCaseFixture.READER_ID))
                .thenReturn(Optional.of(TermUseCaseFixture.readerProfile()));

        final var thrown = catchThrowable(
                () -> useCase.execute(new ListTermsQuery(null, null, 10, false), TermUseCaseFixture.READER_ID));

        assertThat(thrown).isInstanceOf(ForbiddenTermException.class);
        verify(terms, never()).findPageIds(any());
    }

    @Test
    @DisplayName("reverse nulo: não consulta repositórios")
    void rejectsNullReverse() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new ListTermsQuery(null, null, 10, null), TermUseCaseFixture.MASTER_ID));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findById(any());
        verify(terms, never()).findPageIds(any());
    }

    @Test
    @DisplayName("limit inválido: não consulta repositórios")
    void rejectsInvalidLimit() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new ListTermsQuery(null, null, 0, false), TermUseCaseFixture.MASTER_ID));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findById(any());
        verify(terms, never()).findPageIds(any());
    }

}
