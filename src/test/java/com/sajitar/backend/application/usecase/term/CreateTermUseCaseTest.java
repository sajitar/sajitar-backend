package com.sajitar.backend.application.usecase.term;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sajitar.backend.application.command.term.CreateTermCommand;
import com.sajitar.backend.domain.exception.ForbiddenTermException;
import com.sajitar.backend.domain.model.term.Term;
import com.sajitar.backend.domain.model.term.TermDetail;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.port.term.TermRepository;

import jakarta.validation.ConstraintViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateTermUseCase")
class CreateTermUseCaseTest {

    @Mock
    private TermRepository terms;

    @Mock
    private ProfileRepository profiles;

    private CreateTermUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateTermUseCase(terms, profiles, TermUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Perfil MASTER grava uma linha nova com draft true e id gerado")
    void persistsWithDraftTrue() {
        when(profiles.findById(TermUseCaseFixture.MASTER_ID))
                .thenReturn(Optional.of(TermUseCaseFixture.masterProfile()));
        when(terms.save(any(Term.class), any(TermDetail.class))).thenAnswer(invocation -> invocation.getArgument(1));

        final var saved = useCase.execute(TermUseCaseFixture.validCreateCommand(), TermUseCaseFixture.MASTER_ID);

        assertThat(saved.type()).isEqualTo(TermDetail.Type.EN);
        assertThat(saved.content()).isEqualTo("Hello.");
        assertThat(saved.draft()).isTrue();
        assertThat(saved.termId()).isNotNull();
        final var termCaptor = ArgumentCaptor.forClass(Term.class);
        final var detailCaptor = ArgumentCaptor.forClass(TermDetail.class);
        verify(terms).save(termCaptor.capture(), detailCaptor.capture());
        assertThat(termCaptor.getValue().id()).isEqualTo(saved.termId()).isEqualTo(detailCaptor.getValue().termId());
        assertThat(detailCaptor.getValue().draft()).isTrue();
    }

    @Test
    @DisplayName("Perfil sem MASTER lança ForbiddenTermException e não grava")
    void forbiddenWhenNotMaster() {
        when(profiles.findById(TermUseCaseFixture.READER_ID))
                .thenReturn(Optional.of(TermUseCaseFixture.readerProfile()));

        final var thrown = catchThrowable(
                () -> useCase.execute(TermUseCaseFixture.validCreateCommand(), TermUseCaseFixture.READER_ID));

        assertThat(thrown).isInstanceOf(ForbiddenTermException.class);
        verify(terms, never()).save(any(), any());
    }

    @Test
    @DisplayName("Perfil inexistente lança ForbiddenTermException")
    void forbiddenWhenProfileMissing() {
        when(profiles.findById(TermUseCaseFixture.READER_ID)).thenReturn(Optional.empty());

        final var thrown = catchThrowable(
                () -> useCase.execute(TermUseCaseFixture.validCreateCommand(), TermUseCaseFixture.READER_ID));

        assertThat(thrown).isInstanceOf(ForbiddenTermException.class);
        verify(terms, never()).save(any(), any());
    }

    @Test
    @DisplayName("type nulo: não consulta repositórios")
    void rejectsNullType() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new CreateTermCommand(null, "Hello."), TermUseCaseFixture.MASTER_ID));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findById(any());
        verify(terms, never()).save(any(), any());
    }

    @Test
    @DisplayName("content em branco: não consulta repositórios")
    void rejectsBlankContent() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new CreateTermCommand(TermDetail.Type.EN, "   "), TermUseCaseFixture.MASTER_ID));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findById(any());
        verify(terms, never()).save(any(), any());
    }

    @Test
    @DisplayName("content nulo: não consulta repositórios")
    void rejectsNullContent() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new CreateTermCommand(TermDetail.Type.EN, null), TermUseCaseFixture.MASTER_ID));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findById(any());
        verify(terms, never()).save(any(), any());
    }

}
