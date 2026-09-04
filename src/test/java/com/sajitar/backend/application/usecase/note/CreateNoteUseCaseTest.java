package com.sajitar.backend.application.usecase.note;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

import com.sajitar.backend.application.command.note.CreateNoteCommand;
import com.sajitar.backend.domain.exception.ProfileUnavailableException;
import com.sajitar.backend.domain.model.note.Note;
import com.sajitar.backend.domain.port.note.NoteRepository;
import com.sajitar.backend.domain.port.profile.ProfileRepository;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateNoteUseCase")
class CreateNoteUseCaseTest {

    @Mock
    private NoteRepository notes;

    @Mock
    private ProfileRepository profiles;

    private CreateNoteUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateNoteUseCase(notes, profiles, NoteUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Persiste PUBLIC quando o perfil existe")
    void persistsPublic() {
        final var command = NoteUseCaseFixture.validCreateCommand();
        when(profiles.findById(command.profileId())).thenReturn(Optional.of(NoteUseCaseFixture.availableProfile()));
        when(notes.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command);

        assertThat(saved.type()).isEqualTo(Note.Type.PUBLIC);
        assertThat(saved.profileId()).isEqualTo(command.profileId());
        assertThat(saved.content()).isEqualTo(command.content());
        final var captor = ArgumentCaptor.forClass(Note.class);
        verify(notes).save(captor.capture());
        assertThat(captor.getValue().profileId()).isEqualTo(command.profileId());
        assertThat(captor.getValue().id()).isNotNull();
        verify(notes, never()).findById(any());
    }

    @Test
    @DisplayName("Persiste segunda nota PUBLIC do mesmo perfil")
    void persistsSecondPublicForSameProfile() {
        final var command = NoteUseCaseFixture.validCreateCommand();
        when(profiles.findById(command.profileId())).thenReturn(Optional.of(NoteUseCaseFixture.availableProfile()));
        when(notes.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var first = useCase.execute(command);
        final var second = useCase.execute(command);

        assertThat(first.type()).isEqualTo(Note.Type.PUBLIC);
        assertThat(second.type()).isEqualTo(Note.Type.PUBLIC);
        verify(notes, times(2)).save(any(Note.class));
    }

    @Test
    @DisplayName("Persiste PROTECTED quando o perfil existe")
    void persistsProtected() {
        final var command = new CreateNoteCommand(
                NoteUseCaseFixture.PROFILE_ID, Note.Type.PROTECTED, "Protegida.");
        when(profiles.findById(command.profileId())).thenReturn(Optional.of(NoteUseCaseFixture.availableProfile()));
        when(notes.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command);

        assertThat(saved.type()).isEqualTo(Note.Type.PROTECTED);
        assertThat(saved.content()).isEqualTo("Protegida.");
    }

    @Test
    @DisplayName("Perfil inexistente lança ProfileUnavailableException")
    void throwsWhenProfileIsMissing() {
        final var command = NoteUseCaseFixture.validCreateCommand();
        when(profiles.findById(command.profileId())).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(ProfileUnavailableException.class);
        verify(notes, never()).save(any());
    }

    @Test
    @DisplayName("profileId nulo: não consulta repositórios")
    void rejectsNullProfileId() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new CreateNoteCommand(null, Note.Type.PUBLIC, NoteUseCaseFixture.CONTENT)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotNull.class);
        verify(profiles, never()).findById(any());
        verify(notes, never()).save(any());
    }

    @Test
    @DisplayName("type nulo: não consulta repositórios")
    void rejectsNullType() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new CreateNoteCommand(
                        NoteUseCaseFixture.PROFILE_ID, null, NoteUseCaseFixture.CONTENT)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findById(any());
        verify(notes, never()).save(any());
    }

    @Test
    @DisplayName("content em branco: não consulta repositórios")
    void rejectsBlankContent() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new CreateNoteCommand(
                        NoteUseCaseFixture.PROFILE_ID, Note.Type.PUBLIC, "")));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findById(any());
        verify(notes, never()).save(any());
    }

    @Test
    @DisplayName("content nulo: não consulta repositórios")
    void rejectsNullContent() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new CreateNoteCommand(
                        NoteUseCaseFixture.PROFILE_ID, Note.Type.PUBLIC, null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findById(any());
        verify(notes, never()).save(any());
    }

}
