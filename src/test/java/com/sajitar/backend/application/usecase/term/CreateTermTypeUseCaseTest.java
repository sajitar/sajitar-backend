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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sajitar.backend.application.command.term.CreateTermTypeCommand;
import com.sajitar.backend.domain.exception.DuplicateTermTypeException;
import com.sajitar.backend.domain.exception.ForbiddenTermException;
import com.sajitar.backend.domain.exception.TermNotFoundException;
import com.sajitar.backend.domain.model.term.Term;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.port.term.TermRepository;

import jakarta.validation.ConstraintViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateTermTypeUseCase")
class CreateTermTypeUseCaseTest {

    @Mock
    private TermRepository terms;

    @Mock
    private ProfileRepository profiles;

    private CreateTermTypeUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateTermTypeUseCase(terms, profiles, TermUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Acrescenta um type novo com draft true em um id existente")
    void persistsNewType() {
        when(profiles.findById(TermUseCaseFixture.MASTER_ID))
                .thenReturn(Optional.of(TermUseCaseFixture.masterProfile()));
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID))
                .thenReturn(List.of(TermUseCaseFixture.draft(TermUseCaseFixture.TERM_ID, Term.Type.EN)));
        when(terms.save(any(Term.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(TermUseCaseFixture.validCreateTypeCommand(), TermUseCaseFixture.MASTER_ID);

        assertThat(saved.id()).isEqualTo(TermUseCaseFixture.TERM_ID);
        assertThat(saved.type()).isEqualTo(Term.Type.PT);
        assertThat(saved.content()).isEqualTo("Olá.");
        assertThat(saved.draft()).isTrue();
        final var captor = ArgumentCaptor.forClass(Term.class);
        verify(terms).save(captor.capture());
        assertThat(captor.getValue().draft()).isTrue();
    }

    @Test
    @DisplayName("Perfil sem MASTER lança ForbiddenTermException antes de buscar o id")
    void forbiddenWhenNotMaster() {
        when(profiles.findById(TermUseCaseFixture.READER_ID))
                .thenReturn(Optional.of(TermUseCaseFixture.readerProfile()));

        final var thrown = catchThrowable(
                () -> useCase.execute(TermUseCaseFixture.validCreateTypeCommand(), TermUseCaseFixture.READER_ID));

        assertThat(thrown).isInstanceOf(ForbiddenTermException.class);
        verify(terms, never()).findByIdAll(any());
        verify(terms, never()).save(any());
    }

    @Test
    @DisplayName("id ausente lança TermNotFoundException")
    void throwsWhenIdMissing() {
        when(profiles.findById(TermUseCaseFixture.MASTER_ID))
                .thenReturn(Optional.of(TermUseCaseFixture.masterProfile()));
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID)).thenReturn(List.of());

        final var thrown = catchThrowable(
                () -> useCase.execute(TermUseCaseFixture.validCreateTypeCommand(), TermUseCaseFixture.MASTER_ID));

        assertThat(thrown).isInstanceOf(TermNotFoundException.class);
        verify(terms, never()).save(any());
    }

    @Test
    @DisplayName("Par id e type repetido lança DuplicateTermTypeException e preserva a linha")
    void throwsWhenTypeDuplicate() {
        when(profiles.findById(TermUseCaseFixture.MASTER_ID))
                .thenReturn(Optional.of(TermUseCaseFixture.masterProfile()));
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID))
                .thenReturn(List.of(TermUseCaseFixture.draft(TermUseCaseFixture.TERM_ID, Term.Type.PT)));

        final var thrown = catchThrowable(
                () -> useCase.execute(TermUseCaseFixture.validCreateTypeCommand(), TermUseCaseFixture.MASTER_ID));

        assertThat(thrown).isInstanceOf(DuplicateTermTypeException.class);
        verify(terms, never()).save(any());
    }

    @Test
    @DisplayName("id nulo: não consulta repositórios")
    void rejectsNullId() {
        final var thrown = catchThrowable(() -> useCase.execute(
                new CreateTermTypeCommand(null, Term.Type.PT, "Olá."), TermUseCaseFixture.MASTER_ID));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findById(any());
        verify(terms, never()).findByIdAll(any());
    }

    @Test
    @DisplayName("type nulo: não consulta repositórios")
    void rejectsNullType() {
        final var thrown = catchThrowable(() -> useCase.execute(
                new CreateTermTypeCommand(TermUseCaseFixture.TERM_ID, null, "Olá."), TermUseCaseFixture.MASTER_ID));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findById(any());
        verify(terms, never()).findByIdAll(any());
    }

    @Test
    @DisplayName("content em branco: não consulta repositórios")
    void rejectsBlankContent() {
        final var thrown = catchThrowable(() -> useCase.execute(
                new CreateTermTypeCommand(TermUseCaseFixture.TERM_ID, Term.Type.PT, " "), TermUseCaseFixture.MASTER_ID));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findById(any());
        verify(terms, never()).findByIdAll(any());
    }

}
