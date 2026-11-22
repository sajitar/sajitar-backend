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
import com.sajitar.backend.domain.model.term.TermDetail;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.port.term.TermRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteTermUseCase")
class DeleteTermUseCaseTest {

    @Mock
    private TermRepository terms;

    @Mock
    private ProfileRepository profiles;

    private DeleteTermUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteTermUseCase(terms, profiles);
    }

    @Test
    @DisplayName("Apaga todas as linhas do id existente")
    void deletesWhenExists() {
        when(profiles.findById(TermUseCaseFixture.MASTER_ID))
                .thenReturn(Optional.of(TermUseCaseFixture.masterProfile()));
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID))
                .thenReturn(List.of(TermUseCaseFixture.draft(TermUseCaseFixture.TERM_ID, TermDetail.Type.EN)));

        useCase.execute(TermUseCaseFixture.TERM_ID, TermUseCaseFixture.MASTER_ID);

        verify(terms).deleteById(TermUseCaseFixture.TERM_ID);
    }

    @Test
    @DisplayName("id ausente lança TermNotFoundException e não apaga")
    void throwsWhenMissing() {
        when(profiles.findById(TermUseCaseFixture.MASTER_ID))
                .thenReturn(Optional.of(TermUseCaseFixture.masterProfile()));
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID)).thenReturn(List.of());

        final var thrown = catchThrowable(
                () -> useCase.execute(TermUseCaseFixture.TERM_ID, TermUseCaseFixture.MASTER_ID));

        assertThat(thrown).isInstanceOf(TermNotFoundException.class);
        verify(terms, never()).deleteById(any());
    }

    @Test
    @DisplayName("Perfil sem MASTER lança ForbiddenTermException antes de buscar")
    void forbiddenWhenNotMaster() {
        when(profiles.findById(TermUseCaseFixture.READER_ID))
                .thenReturn(Optional.of(TermUseCaseFixture.readerProfile()));

        final var thrown = catchThrowable(
                () -> useCase.execute(TermUseCaseFixture.TERM_ID, TermUseCaseFixture.READER_ID));

        assertThat(thrown).isInstanceOf(ForbiddenTermException.class);
        verify(terms, never()).findByIdAll(any());
        verify(terms, never()).deleteById(any());
    }

}
