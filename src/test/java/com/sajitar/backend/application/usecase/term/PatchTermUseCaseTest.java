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

import com.sajitar.backend.application.command.PatchValue;
import com.sajitar.backend.application.command.term.PatchTermCommand;
import com.sajitar.backend.domain.exception.ForbiddenTermException;
import com.sajitar.backend.domain.exception.InvalidTermDraftException;
import com.sajitar.backend.domain.exception.TermNotFoundException;
import com.sajitar.backend.domain.exception.TermTypeAbsentException;
import com.sajitar.backend.domain.model.term.Term;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.port.term.TermRepository;

import jakarta.validation.ConstraintViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("PatchTermUseCase")
class PatchTermUseCaseTest {

    @Mock
    private TermRepository terms;

    @Mock
    private ProfileRepository profiles;

    private PatchTermUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new PatchTermUseCase(terms, profiles, TermUseCaseFixture.VALIDATOR);
    }

    private void withMaster() {
        when(profiles.findById(TermUseCaseFixture.MASTER_ID))
                .thenReturn(Optional.of(TermUseCaseFixture.masterProfile()));
    }

    @Test
    @DisplayName("draft ausente altera o content e mantém draft true")
    void patchesContentKeepingDraft() {
        withMaster();
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID))
                .thenReturn(List.of(TermUseCaseFixture.draft(TermUseCaseFixture.TERM_ID, Term.Type.EN)));
        when(terms.save(any(Term.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(
                new PatchTermCommand(TermUseCaseFixture.TERM_ID, Term.Type.EN, "Novo texto.", PatchValue.absent()),
                TermUseCaseFixture.MASTER_ID);

        assertThat(saved.content()).isEqualTo("Novo texto.");
        assertThat(saved.draft()).isTrue();
    }

    @Test
    @DisplayName("draft nulo no construtor é normalizado para ausente e mantém draft true")
    void normalizesNullDraft() {
        withMaster();
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID))
                .thenReturn(List.of(TermUseCaseFixture.draft(TermUseCaseFixture.TERM_ID, Term.Type.EN)));
        when(terms.save(any(Term.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(
                new PatchTermCommand(TermUseCaseFixture.TERM_ID, Term.Type.EN, "Texto.", null),
                TermUseCaseFixture.MASTER_ID);

        assertThat(saved.draft()).isTrue();
    }

    @Test
    @DisplayName("draft true no corpo é ignorado e mantém draft true")
    void ignoresDraftTrueInBody() {
        withMaster();
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID))
                .thenReturn(List.of(TermUseCaseFixture.draft(TermUseCaseFixture.TERM_ID, Term.Type.EN)));
        when(terms.save(any(Term.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(
                new PatchTermCommand(TermUseCaseFixture.TERM_ID, Term.Type.EN, "Texto.", PatchValue.of(true)),
                TermUseCaseFixture.MASTER_ID);

        assertThat(saved.draft()).isTrue();
    }

    @Test
    @DisplayName("draft false com EN, PT e ES publica a linha")
    void publishesWhenAllTypesPresent() {
        withMaster();
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID)).thenReturn(List.of(
                TermUseCaseFixture.draft(TermUseCaseFixture.TERM_ID, Term.Type.EN),
                TermUseCaseFixture.draft(TermUseCaseFixture.TERM_ID, Term.Type.PT),
                TermUseCaseFixture.draft(TermUseCaseFixture.TERM_ID, Term.Type.ES)));
        when(terms.save(any(Term.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(
                new PatchTermCommand(TermUseCaseFixture.TERM_ID, Term.Type.EN, "Final.", PatchValue.of(false)),
                TermUseCaseFixture.MASTER_ID);

        assertThat(saved.draft()).isFalse();
        assertThat(saved.content()).isEqualTo("Final.");
    }

    @Test
    @DisplayName("Perfil sem MASTER lança ForbiddenTermException e não grava")
    void forbiddenWhenNotMaster() {
        when(profiles.findById(TermUseCaseFixture.READER_ID))
                .thenReturn(Optional.of(TermUseCaseFixture.readerProfile()));

        final var thrown = catchThrowable(() -> useCase.execute(
                new PatchTermCommand(TermUseCaseFixture.TERM_ID, Term.Type.EN, "Texto.", PatchValue.absent()),
                TermUseCaseFixture.READER_ID));

        assertThat(thrown).isInstanceOf(ForbiddenTermException.class);
        verify(terms, never()).findByIdAll(any());
        verify(terms, never()).save(any());
    }

    @Test
    @DisplayName("id ausente lança TermNotFoundException")
    void throwsWhenIdMissing() {
        withMaster();
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID)).thenReturn(List.of());

        final var thrown = catchThrowable(() -> useCase.execute(
                new PatchTermCommand(TermUseCaseFixture.TERM_ID, Term.Type.EN, "Texto.", PatchValue.absent()),
                TermUseCaseFixture.MASTER_ID));

        assertThat(thrown).isInstanceOf(TermNotFoundException.class);
        verify(terms, never()).save(any());
    }

    @Test
    @DisplayName("type ausente no id lança TermTypeAbsentException")
    void throwsWhenTypeAbsent() {
        withMaster();
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID))
                .thenReturn(List.of(TermUseCaseFixture.draft(TermUseCaseFixture.TERM_ID, Term.Type.EN)));

        final var thrown = catchThrowable(() -> useCase.execute(
                new PatchTermCommand(TermUseCaseFixture.TERM_ID, Term.Type.PT, "Texto.", PatchValue.absent()),
                TermUseCaseFixture.MASTER_ID));

        assertThat(thrown).isInstanceOf(TermTypeAbsentException.class);
        verify(terms, never()).save(any());
    }

    @Test
    @DisplayName("Linha com draft false recusa o patch com 400 draft")
    void throwsWhenLineClosed() {
        withMaster();
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID))
                .thenReturn(List.of(TermUseCaseFixture.published(TermUseCaseFixture.TERM_ID, Term.Type.EN)));

        final var thrown = catchThrowable(() -> useCase.execute(
                new PatchTermCommand(TermUseCaseFixture.TERM_ID, Term.Type.EN, "Texto.", PatchValue.absent()),
                TermUseCaseFixture.MASTER_ID));

        assertThat(thrown).isInstanceOf(InvalidTermDraftException.class);
        assertThat(((InvalidTermDraftException) thrown).content().get("draft"))
                .containsExactly(InvalidTermDraftException.CLOSED_KEY);
        verify(terms, never()).save(any());
    }

    @Test
    @DisplayName("draft false sem os três Type recusa com 400 draft")
    void throwsWhenPublishIncomplete() {
        withMaster();
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID)).thenReturn(List.of(
                TermUseCaseFixture.draft(TermUseCaseFixture.TERM_ID, Term.Type.EN),
                TermUseCaseFixture.draft(TermUseCaseFixture.TERM_ID, Term.Type.PT)));

        final var thrown = catchThrowable(() -> useCase.execute(
                new PatchTermCommand(TermUseCaseFixture.TERM_ID, Term.Type.EN, "Texto.", PatchValue.of(false)),
                TermUseCaseFixture.MASTER_ID));

        assertThat(thrown).isInstanceOf(InvalidTermDraftException.class);
        assertThat(((InvalidTermDraftException) thrown).content().get("draft"))
                .containsExactly(InvalidTermDraftException.INCOMPLETE_KEY);
        verify(terms, never()).save(any());
    }

    @Test
    @DisplayName("draft nulo recusa com 400 draft")
    void throwsWhenDraftNull() {
        withMaster();
        when(terms.findByIdAll(TermUseCaseFixture.TERM_ID))
                .thenReturn(List.of(TermUseCaseFixture.draft(TermUseCaseFixture.TERM_ID, Term.Type.EN)));

        final var thrown = catchThrowable(() -> useCase.execute(
                new PatchTermCommand(TermUseCaseFixture.TERM_ID, Term.Type.EN, "Texto.", PatchValue.of(null)),
                TermUseCaseFixture.MASTER_ID));

        assertThat(thrown).isInstanceOf(InvalidTermDraftException.class);
        assertThat(((InvalidTermDraftException) thrown).content().get("draft"))
                .containsExactly(InvalidTermDraftException.NULL_KEY);
        verify(terms, never()).save(any());
    }

    @Test
    @DisplayName("id nulo: não consulta repositórios")
    void rejectsNullId() {
        final var thrown = catchThrowable(() -> useCase.execute(
                new PatchTermCommand(null, Term.Type.EN, "Texto.", PatchValue.absent()), TermUseCaseFixture.MASTER_ID));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findById(any());
        verify(terms, never()).findByIdAll(any());
    }

    @Test
    @DisplayName("type nulo: não consulta repositórios")
    void rejectsNullType() {
        final var thrown = catchThrowable(() -> useCase.execute(
                new PatchTermCommand(TermUseCaseFixture.TERM_ID, null, "Texto.", PatchValue.absent()),
                TermUseCaseFixture.MASTER_ID));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findById(any());
        verify(terms, never()).findByIdAll(any());
    }

    @Test
    @DisplayName("content em branco: não consulta repositórios")
    void rejectsBlankContent() {
        final var thrown = catchThrowable(() -> useCase.execute(
                new PatchTermCommand(TermUseCaseFixture.TERM_ID, Term.Type.EN, "  ", PatchValue.absent()),
                TermUseCaseFixture.MASTER_ID));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findById(any());
        verify(terms, never()).findByIdAll(any());
    }

}
