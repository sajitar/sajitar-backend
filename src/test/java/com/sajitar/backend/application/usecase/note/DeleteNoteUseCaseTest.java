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

import com.sajitar.backend.application.command.note.DeleteNoteCommand;
import com.sajitar.backend.domain.exception.NoteNotFoundException;
import com.sajitar.backend.domain.port.note.NoteRepository;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteNoteUseCase")
class DeleteNoteUseCaseTest {

    @Mock
    private NoteRepository notes;

    private DeleteNoteUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteNoteUseCase(notes, NoteUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Remove a nota")
    void deletesWhenPresent() {
        final var existing = NoteUseCaseFixture.persistedPublic();
        when(notes.findById(existing.id())).thenReturn(Optional.of(existing));

        useCase.execute(new DeleteNoteCommand(existing.id()));

        verify(notes).deleteById(existing.id());
    }

    @Test
    @DisplayName("Remove PROTECTED")
    void deletesProtected() {
        final var existing = NoteUseCaseFixture.persistedProtected();
        when(notes.findById(existing.id())).thenReturn(Optional.of(existing));

        useCase.execute(new DeleteNoteCommand(existing.id()));

        verify(notes).deleteById(existing.id());
    }

    @Test
    @DisplayName("Lança NoteNotFoundException quando o id não existe")
    void throwsWhenMissing() {
        when(notes.findById(NoteUseCaseFixture.ID)).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(new DeleteNoteCommand(NoteUseCaseFixture.ID)));

        assertThat(thrown).isInstanceOf(NoteNotFoundException.class);
        verify(notes, never()).deleteById(any());
    }

    @Test
    @DisplayName("Id nulo: não consulta o repositório")
    void rejectsNullId() {
        final var thrown = catchThrowable(() -> useCase.execute(new DeleteNoteCommand(null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotNull.class);
        verify(notes, never()).findById(any(UUID.class));
    }

}
