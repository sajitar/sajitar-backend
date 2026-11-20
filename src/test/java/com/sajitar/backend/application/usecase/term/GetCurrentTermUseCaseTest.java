package com.sajitar.backend.application.usecase.term;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sajitar.backend.domain.exception.TermNotFoundException;
import com.sajitar.backend.domain.model.term.Term;
import com.sajitar.backend.domain.port.term.TermRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetCurrentTermUseCase")
class GetCurrentTermUseCaseTest {

    @Mock
    private TermRepository terms;

    private GetCurrentTermUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetCurrentTermUseCase(terms);
    }

    @Test
    @DisplayName("lang ausente devolve as três linhas publicadas do vigente")
    void returnsAllLinesWhenLangAbsent() {
        final var en = TermUseCaseFixture.published(TermUseCaseFixture.TERM_ID, Term.Type.EN);
        final var pt = TermUseCaseFixture.published(TermUseCaseFixture.TERM_ID, Term.Type.PT);
        final var es = TermUseCaseFixture.published(TermUseCaseFixture.TERM_ID, Term.Type.ES);
        when(terms.findCurrentId()).thenReturn(Optional.of(TermUseCaseFixture.TERM_ID));
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID)).thenReturn(List.of(en, pt, es));

        final var lines = useCase.execute(null);

        assertThat(lines).containsExactly(en, pt, es);
    }

    @Test
    @DisplayName("lang presente devolve só a linha do type do vigente")
    void returnsSingleLineWhenLangPresent() {
        final var en = TermUseCaseFixture.published(TermUseCaseFixture.TERM_ID, Term.Type.EN);
        final var pt = TermUseCaseFixture.published(TermUseCaseFixture.TERM_ID, Term.Type.PT);
        final var es = TermUseCaseFixture.published(TermUseCaseFixture.TERM_ID, Term.Type.ES);
        when(terms.findCurrentId()).thenReturn(Optional.of(TermUseCaseFixture.TERM_ID));
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID)).thenReturn(List.of(en, pt, es));

        final var lines = useCase.execute(Term.Type.ES);

        assertThat(lines).containsExactly(es);
    }

    @Test
    @DisplayName("Sem vigente lança TermNotFoundException")
    void throwsWhenNoCurrent() {
        when(terms.findCurrentId()).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(null));

        assertThat(thrown).isInstanceOf(TermNotFoundException.class);
        verify(terms, never()).findByIdAll(any());
    }

}
