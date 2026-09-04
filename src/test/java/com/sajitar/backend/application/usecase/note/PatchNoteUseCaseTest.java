package com.sajitar.backend.application.usecase.note;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sajitar.backend.application.command.PatchValue;
import com.sajitar.backend.application.command.note.PatchNoteCommand;
import com.sajitar.backend.domain.exception.NoteNotFoundException;
import com.sajitar.backend.domain.model.note.Note;
import com.sajitar.backend.domain.port.note.NoteRepository;
import com.sajitar.backend.domain.validation.note.Content;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;

@ExtendWith(MockitoExtension.class)
@DisplayName("PatchNoteUseCase")
class PatchNoteUseCaseTest {

    @Mock
    private NoteRepository notes;

    private PatchNoteUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new PatchNoteUseCase(notes, NoteUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Atualiza o type quando informado")
    void patchesType() {
        final var existing = NoteUseCaseFixture.persistedPublic();
        when(notes.findById(existing.id())).thenReturn(Optional.of(existing));
        when(notes.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(new PatchNoteCommand(existing.id(), Note.Type.PROTECTED, null));

        assertThat(saved.id()).isEqualTo(existing.id());
        assertThat(saved.profileId()).isEqualTo(existing.profileId());
        assertThat(saved.type()).isEqualTo(Note.Type.PROTECTED);
        assertThat(saved.content()).isEqualTo(existing.content());
    }

    @Test
    @DisplayName("Atualiza só o content quando informado")
    void patchesContent() {
        final var existing = NoteUseCaseFixture.persistedPublic();
        when(notes.findById(existing.id())).thenReturn(Optional.of(existing));
        when(notes.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(
                new PatchNoteCommand(existing.id(), null, PatchValue.of("Texto parcial.")));

        assertThat(saved.type()).isEqualTo(existing.type());
        assertThat(saved.content()).isEqualTo("Texto parcial.");
    }

    @Test
    @DisplayName("content nulo não consulta o repositório")
    void rejectsPresentNullContent() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new PatchNoteCommand(NoteUseCaseFixture.ID, null, PatchValue.of(null))));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(notes, never()).findById(any());
        verify(notes, never()).save(any());
    }

    @Test
    @DisplayName("content vazio não consulta o repositório")
    void rejectsEmptyContent() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new PatchNoteCommand(NoteUseCaseFixture.ID, null, PatchValue.of(""))));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(notes, never()).findById(any());
        verify(notes, never()).save(any());
    }

    @Test
    @DisplayName("Type igual ao persistido não grava")
    void sameTypeDoesNotSave() {
        final var existing = NoteUseCaseFixture.persistedPublic();
        when(notes.findById(existing.id())).thenReturn(Optional.of(existing));

        final var saved = useCase.execute(new PatchNoteCommand(existing.id(), existing.type(), null));

        assertThat(saved).isEqualTo(existing);
        verify(notes, never()).save(any());
    }

    @Test
    @DisplayName("Content igual ao persistido não grava")
    void sameContentDoesNotSave() {
        final var existing = NoteUseCaseFixture.persistedPublic();
        when(notes.findById(existing.id())).thenReturn(Optional.of(existing));

        final var saved = useCase.execute(
                new PatchNoteCommand(existing.id(), null, PatchValue.of(existing.content())));

        assertThat(saved).isEqualTo(existing);
        verify(notes, never()).save(any());
    }

    @Test
    @DisplayName("Body vazio não persiste e devolve o estado atual")
    void emptyPatchDoesNotSave() {
        final var existing = NoteUseCaseFixture.persistedPublic();
        when(notes.findById(existing.id())).thenReturn(Optional.of(existing));

        final var saved = useCase.execute(NoteUseCaseFixture.emptyPatchCommand());

        assertThat(saved).isEqualTo(existing);
        verify(notes, never()).save(any());
    }

    @Test
    @DisplayName("content acima de 1000 caracteres não consulta o repositório")
    void rejectsTooLongContent() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new PatchNoteCommand(
                        NoteUseCaseFixture.ID, null, PatchValue.of("a".repeat(Content.MAX_SIZE + 1)))));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(notes, never()).findById(any());
        verify(notes, never()).save(any());
    }

    @Test
    @DisplayName("Lança NoteNotFoundException quando o id não existe")
    void throwsWhenMissing() {
        when(notes.findById(NoteUseCaseFixture.ID)).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(NoteUseCaseFixture.emptyPatchCommand()));

        assertThat(thrown).isInstanceOf(NoteNotFoundException.class);
        verify(notes, never()).save(any());
    }

    @Test
    @DisplayName("Id nulo: não consulta o repositório")
    void rejectsNullId() {
        final var thrown = catchThrowable(() -> useCase.execute(new PatchNoteCommand(null, null, null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotNull.class);
        verify(notes, never()).findById(any(UUID.class));
    }

    @Test
    @DisplayName("hasChanges cobre type e content presentes ou ausentes")
    void hasChangesSemantics() {
        assertThat(NoteUseCaseFixture.emptyPatchCommand().hasChanges()).isFalse();
        assertThat(new PatchNoteCommand(NoteUseCaseFixture.ID, Note.Type.PUBLIC, null).hasChanges()).isTrue();
        assertThat(new PatchNoteCommand(NoteUseCaseFixture.ID, null, PatchValue.of("x")).hasChanges()).isTrue();
        assertThat(new PatchNoteCommand(NoteUseCaseFixture.ID, null, PatchValue.of(null)).hasChanges()).isTrue();
        assertThat(new PatchNoteCommand(NoteUseCaseFixture.ID, null, PatchValue.absent()).hasChanges()).isFalse();
    }

}
