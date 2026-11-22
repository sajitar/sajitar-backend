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

import com.sajitar.backend.domain.exception.ForbiddenTermException;
import com.sajitar.backend.domain.exception.TermNotFoundException;
import com.sajitar.backend.domain.exception.TermTypeAbsentException;
import com.sajitar.backend.domain.model.term.Term;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.port.term.TermRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetTermUseCase")
class GetTermUseCaseTest {

    @Mock
    private TermRepository terms;

    @Mock
    private ProfileRepository profiles;

    private GetTermUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetTermUseCase(terms, profiles);
    }

    private void withMaster() {
        when(profiles.findById(TermUseCaseFixture.MASTER_ID))
                .thenReturn(Optional.of(TermUseCaseFixture.masterProfile()));
    }

    @Test
    @DisplayName("lang ausente devolve todas as linhas do id")
    void returnsAllLinesWhenLangAbsent() {
        withMaster();
        final var en = TermUseCaseFixture.draft(TermUseCaseFixture.TERM_ID, Term.Type.EN);
        final var pt = TermUseCaseFixture.draft(TermUseCaseFixture.TERM_ID, Term.Type.PT);
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID)).thenReturn(List.of(en, pt));

        final var lines = useCase.execute(TermUseCaseFixture.TERM_ID, null, TermUseCaseFixture.MASTER_ID);

        assertThat(lines).containsExactly(en, pt);
    }

    @Test
    @DisplayName("lang presente devolve só a linha do type")
    void returnsSingleLineWhenLangPresent() {
        withMaster();
        final var en = TermUseCaseFixture.draft(TermUseCaseFixture.TERM_ID, Term.Type.EN);
        final var pt = TermUseCaseFixture.draft(TermUseCaseFixture.TERM_ID, Term.Type.PT);
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID)).thenReturn(List.of(en, pt));

        final var lines = useCase.execute(TermUseCaseFixture.TERM_ID, Term.Type.PT, TermUseCaseFixture.MASTER_ID);

        assertThat(lines).containsExactly(pt);
    }

    @Test
    @DisplayName("id ausente lança TermNotFoundException")
    void throwsWhenIdMissing() {
        withMaster();
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID)).thenReturn(List.of());

        final var thrown = catchThrowable(
                () -> useCase.execute(TermUseCaseFixture.TERM_ID, null, TermUseCaseFixture.MASTER_ID));

        assertThat(thrown).isInstanceOf(TermNotFoundException.class);
    }

    @Test
    @DisplayName("type ausente no id lança TermTypeAbsentException")
    void throwsWhenTypeAbsent() {
        withMaster();
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID))
                .thenReturn(List.of(TermUseCaseFixture.draft(TermUseCaseFixture.TERM_ID, Term.Type.EN)));

        final var thrown = catchThrowable(
                () -> useCase.execute(TermUseCaseFixture.TERM_ID, Term.Type.ES, TermUseCaseFixture.MASTER_ID));

        assertThat(thrown).isInstanceOf(TermTypeAbsentException.class);
    }

    @Test
    @DisplayName("Perfil sem MASTER lança ForbiddenTermException antes de buscar")
    void forbiddenWhenNotMaster() {
        when(profiles.findById(TermUseCaseFixture.READER_ID))
                .thenReturn(Optional.of(TermUseCaseFixture.readerProfile()));

        final var thrown = catchThrowable(
                () -> useCase.execute(TermUseCaseFixture.TERM_ID, null, TermUseCaseFixture.READER_ID));

        assertThat(thrown).isInstanceOf(ForbiddenTermException.class);
        verify(terms, never()).findByIdAll(any());
    }

}
