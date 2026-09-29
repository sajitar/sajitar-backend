package com.sajitar.backend.application.usecase.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sajitar.backend.application.command.PatchValue;
import com.sajitar.backend.application.command.profile.PatchProfileCommand;
import com.sajitar.backend.domain.exception.ForbiddenProfileTypeException;
import com.sajitar.backend.domain.exception.ProfileNotFoundException;
import com.sajitar.backend.domain.model.profile.Profile;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.validation.Limit;
import com.sajitar.backend.domain.validation.profile.Birthday;
import com.sajitar.backend.domain.validation.profile.Description;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@ExtendWith(MockitoExtension.class)
@DisplayName("PatchProfileUseCase")
class PatchProfileUseCaseTest {

    @Mock
    private ProfileRepository profiles;

    private PatchProfileUseCase useCase;

    @BeforeAll
    static void configureValidation() {
        Birthday.BirthdayValidator.configure(18);
        Limit.LimitValidator.configure(100);
    }

    @BeforeEach
    void setUp() {
        useCase = new PatchProfileUseCase(profiles, ProfileUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Atualiza só o nome e mantém o id persistido")
    void patchesOnlyName() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new PatchProfileCommand(
                existing.id(),
                null,
                PatchValue.of("Nome Atualizado"),
                null,
                null);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(profiles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID);

        assertThat(saved.id()).isEqualTo(existing.id());
        assertThat(saved.name()).isEqualTo("Nome Atualizado");
        assertThat(saved.description()).isEqualTo(existing.description());
        assertThat(saved.birthday()).isEqualTo(existing.birthday());
        assertThat(saved.email()).isEqualTo(existing.email());
        assertThat(saved.password()).isEqualTo(existing.password());
        assertThat(saved.type()).isEqualTo(existing.type());
        verify(profiles, never()).findByEmail(any());
    }

    @Test
    @DisplayName("Atualiza só a descrição")
    void patchesOnlyDescription() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new PatchProfileCommand(
                existing.id(),
                null,
                null,
                PatchValue.of("Nova descricao"),
                null);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(profiles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID);

        assertThat(saved.description()).isEqualTo("Nova descricao");
        assertThat(saved.name()).isEqualTo(existing.name());
    }

    @Test
    @DisplayName("description nula remove a descrição atual")
    void clearsDescriptionWhenPresentNull() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new PatchProfileCommand(
                existing.id(),
                null,
                null,
                PatchValue.of(null),
                null);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(profiles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID);

        assertThat(saved.description()).isNull();
    }

    @Test
    @DisplayName("Lança ProfileNotFoundException quando o id não existe")
    void throwsWhenProfileDoesNotExist() {
        final var command = ProfileUseCaseFixture.emptyPatchCommand();
        when(profiles.findById(command.id())).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID));

        assertThat(thrown).isInstanceOf(ProfileNotFoundException.class);
        verify(profiles).findById(command.id());
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("Nome inválido presente não consulta o repositório")
    void doesNotTouchRepositoryWhenPresentNameIsInvalid() {
        final var command = new PatchProfileCommand(
                ProfileUseCaseFixture.ID,
                null,
                PatchValue.of("123"),
                null,
                null);

        final var thrown = catchThrowable(() -> useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(Pattern.class);
        verify(profiles, never()).findById(any());
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("Patch vazio persiste o existente com o mesmo id")
    void emptyPatchPersistsExistingWithSameId() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(profiles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(ProfileUseCaseFixture.emptyPatchCommand(), ProfileUseCaseFixture.VIEWER_ID);

        assertThat(saved).isEqualTo(existing);
        assertThat(saved.id()).isEqualTo(existing.id());
        assertThat(saved.name()).isEqualTo(existing.name());
        assertThat(saved.description()).isEqualTo(existing.description());
        assertThat(saved.birthday()).isEqualTo(existing.birthday());
        assertThat(saved.email()).isEqualTo(existing.email());
        assertThat(saved.password()).isEqualTo(existing.password());
        assertThat(saved.type()).isEqualTo(existing.type());
        final var captor = ArgumentCaptor.forClass(com.sajitar.backend.domain.model.profile.Profile.class);
        verify(profiles).save(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo(existing.id());
    }

    @Test
    @DisplayName("Atualiza só a data de nascimento")
    void patchesOnlyBirthday() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var birthday = LocalDate.parse("1980-05-20");
        final var command = new PatchProfileCommand(
                existing.id(),
                null,
                null,
                null,
                PatchValue.of(birthday));
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(profiles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID);

        assertThat(saved.birthday()).isEqualTo(birthday);
        assertThat(saved.name()).isEqualTo(existing.name());
    }

    @Test
    @DisplayName("Id nulo: violação @NotNull no command")
    void rejectsNullId() {
        final var command = new PatchProfileCommand(null, null, null, null, null);

        final var thrown = catchThrowable(() -> useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotNull.class);
        verify(profiles, never()).findById(any());
    }

    @Test
    @DisplayName("Descrição presente inválida não consulta o repositório")
    void doesNotTouchRepositoryWhenPresentDescriptionIsInvalid() {
        final var command = new PatchProfileCommand(
                ProfileUseCaseFixture.ID,
                null,
                null,
                PatchValue.of("x".repeat(Description.MAX_SIZE + 1)),
                null);

        final var thrown = catchThrowable(() -> useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findById(any());
    }

    @Test
    @DisplayName("Nascimento presente abaixo da idade mínima não consulta o repositório")
    void doesNotTouchRepositoryWhenPresentBirthdayIsInvalid() {
        final var command = new PatchProfileCommand(
                ProfileUseCaseFixture.ID,
                null,
                null,
                null,
                PatchValue.of(LocalDate.now().minusYears(10)));

        final var thrown = catchThrowable(() -> useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findById(any());
    }

    @Test
    @DisplayName("Atualiza só o tipo e mantém os demais campos")
    void patchesOnlyType() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new PatchProfileCommand(
                existing.id(),
                Profile.Type.MASTER,
                null,
                null,
                null);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(profiles.findById(ProfileUseCaseFixture.VIEWER_ID))
                .thenReturn(Optional.of(ProfileUseCaseFixture.persistedMaster(ProfileUseCaseFixture.VIEWER_ID)));
        when(profiles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID);

        assertThat(saved.type()).isEqualTo(Profile.Type.MASTER);
        assertThat(saved.name()).isEqualTo(existing.name());
        assertThat(saved.description()).isEqualTo(existing.description());
        assertThat(saved.birthday()).isEqualTo(existing.birthday());
    }

    @Test
    @DisplayName("Caller que não é MASTER não troca o tipo")
    void nonMasterCannotChangeType() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new PatchProfileCommand(
                existing.id(),
                Profile.Type.MASTER,
                null,
                null,
                null);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(profiles.findById(ProfileUseCaseFixture.VIEWER_ID))
                .thenReturn(Optional.of(ProfileUseCaseFixture.persistedProfile().withId(ProfileUseCaseFixture.VIEWER_ID)));

        final var thrown = catchThrowable(() -> useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID));

        assertThat(thrown).isInstanceOf(ForbiddenProfileTypeException.class);
        assertThat(((ForbiddenProfileTypeException) thrown).content().get("type"))
                .containsExactly(ForbiddenProfileTypeException.MESSAGE_KEY);
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("Viewer nulo não troca o tipo")
    void nullViewerCannotChangeType() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new PatchProfileCommand(
                existing.id(),
                Profile.Type.WRITER,
                null,
                null,
                null);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));

        final var thrown = catchThrowable(() -> useCase.execute(command, null));

        assertThat(thrown).isInstanceOf(ForbiddenProfileTypeException.class);
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("Viewer inexistente não troca o tipo")
    void missingViewerCannotChangeType() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new PatchProfileCommand(
                existing.id(),
                Profile.Type.MASTER,
                null,
                null,
                null);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(profiles.findById(ProfileUseCaseFixture.VIEWER_ID)).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID));

        assertThat(thrown).isInstanceOf(ForbiddenProfileTypeException.class);
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("Caller que não é MASTER persiste quando o type é o vigente")
    void nonMasterKeepsSameType() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new PatchProfileCommand(
                existing.id(),
                existing.type(),
                PatchValue.of("Nome Atualizado"),
                null,
                null);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(profiles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID);

        assertThat(saved.name()).isEqualTo("Nome Atualizado");
        assertThat(saved.type()).isEqualTo(existing.type());
        verify(profiles, never()).findById(ProfileUseCaseFixture.VIEWER_ID);
    }

}
