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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sajitar.backend.application.command.note.UpdateNoteCommand;
import com.sajitar.backend.domain.exception.NoteNotFoundException;
import com.sajitar.backend.domain.model.note.Note;
import com.sajitar.backend.domain.port.note.NoteRepository;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateNoteUseCase")
class UpdateNoteUseCaseTest {

    @Mock
    private NoteRepository notes;

    private UpdateNoteUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateNoteUseCase(notes, NoteUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Troca de tipo persiste com o type novo e id/profileId iguais")
    void appliesTypeChange() {
        final var existing = NoteUseCaseFixture.persistedPublic();
        when(notes.findById(existing.id())).thenReturn(Optional.of(existing));
        when(notes.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(
                new UpdateNoteCommand(existing.id(), Note.Type.PRIVATE, existing.content()));

        assertThat(saved.id()).isEqualTo(existing.id());
        assertThat(saved.profileId()).isEqualTo(existing.profileId());
        assertThat(saved.type()).isEqualTo(Note.Type.PRIVATE);
        assertThat(saved.content()).isEqualTo(existing.content());
        final var captor = ArgumentCaptor.forClass(Note.class);
        verify(notes).save(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo(existing.id());
    }

    @Test
    @DisplayName("Troca só do content persiste o texto novo")
    void appliesContentChange() {
        final var existing = NoteUseCaseFixture.persistedPublic();
        when(notes.findById(existing.id())).thenReturn(Optional.of(existing));
        when(notes.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(
                new UpdateNoteCommand(existing.id(), existing.type(), "Texto novo."));

        assertThat(saved.type()).isEqualTo(existing.type());
        assertThat(saved.content()).isEqualTo("Texto novo.");
        verify(notes).save(any(Note.class));
    }

    @Test
    @DisplayName("Valores idênticos não persistem")
    void identicalValuesDoNotSave() {
        final var existing = NoteUseCaseFixture.persistedPublic();
        when(notes.findById(existing.id())).thenReturn(Optional.of(existing));

        final var saved = useCase.execute(NoteUseCaseFixture.identicalUpdateCommand());

        assertThat(saved).isEqualTo(existing);
        verify(notes, never()).save(any());
    }

    @Test
    @DisplayName("Lança NoteNotFoundException quando o id não existe")
    void throwsWhenMissing() {
        when(notes.findById(NoteUseCaseFixture.ID)).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(NoteUseCaseFixture.identicalUpdateCommand()));

        assertThat(thrown).isInstanceOf(NoteNotFoundException.class);
        verify(notes, never()).save(any());
    }

    @Test
    @DisplayName("type nulo: não consulta o repositório")
    void rejectsNullType() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new UpdateNoteCommand(NoteUseCaseFixture.ID, null, NoteUseCaseFixture.CONTENT)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotNull.class);
        verify(notes, never()).findById(any());
    }

    @Test
    @DisplayName("content em branco: não consulta o repositório")
    void rejectsBlankContent() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new UpdateNoteCommand(NoteUseCaseFixture.ID, Note.Type.PUBLIC, "")));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(notes, never()).findById(any());
    }

    @Test
    @DisplayName("content nulo: não consulta o repositório")
    void rejectsNullContent() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new UpdateNoteCommand(NoteUseCaseFixture.ID, Note.Type.PUBLIC, null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(notes, never()).findById(any());
    }

    @Test
    @DisplayName("Id nulo: não consulta o repositório")
    void rejectsNullId() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new UpdateNoteCommand(null, Note.Type.PUBLIC, NoteUseCaseFixture.CONTENT)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotNull.class);
        verify(notes, never()).findById(any(UUID.class));
    }

}
