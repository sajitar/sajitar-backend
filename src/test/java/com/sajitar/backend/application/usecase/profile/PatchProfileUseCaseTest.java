package com.sajitar.backend.application.usecase.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

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
import com.sajitar.backend.domain.exception.EmailAlreadyRegisteredException;
import com.sajitar.backend.domain.exception.ProfileNotFoundException;
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
                PatchValue.of("Nome Atualizado"),
                null,
                null,
                null);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(profiles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command);

        assertThat(saved.id()).isEqualTo(existing.id());
        assertThat(saved.name()).isEqualTo("Nome Atualizado");
        assertThat(saved.description()).isEqualTo(existing.description());
        assertThat(saved.birthday()).isEqualTo(existing.birthday());
        assertThat(saved.email()).isEqualTo(existing.email());
        assertThat(saved.password()).isEqualTo(existing.password());
        verify(profiles, never()).findByEmail(any());
    }

    @Test
    @DisplayName("Atualiza só a descrição")
    void patchesOnlyDescription() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new PatchProfileCommand(
                existing.id(),
                null,
                PatchValue.of("Nova descricao"),
                null,
                null);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(profiles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command);

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
                PatchValue.of(null),
                null,
                null);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(profiles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command);

        assertThat(saved.description()).isNull();
    }

    @Test
    @DisplayName("Lança ProfileNotFoundException quando o id não existe")
    void throwsWhenProfileDoesNotExist() {
        final var command = ProfileUseCaseFixture.emptyPatchCommand();
        when(profiles.findById(command.id())).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(ProfileNotFoundException.class);
        verify(profiles).findById(command.id());
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("Lança EmailAlreadyRegisteredException quando o e-mail pertence a outro perfil")
    void throwsWhenEmailBelongsToAnotherProfile() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var other = existing.withId(UUID.randomUUID()).withEmail("other@example.com");
        final var command = new PatchProfileCommand(
                existing.id(),
                null,
                null,
                null,
                PatchValue.of(other.email()));
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(profiles.findByEmail(other.email())).thenReturn(Optional.of(other));

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(EmailAlreadyRegisteredException.class);
        assertThat(((EmailAlreadyRegisteredException) thrown).content().get("email"))
                .containsExactly(EmailAlreadyRegisteredException.MESSAGE_KEY);
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("E-mail omitido não consulta findByEmail")
    void omittedEmailDoesNotLookupEmail() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(profiles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(ProfileUseCaseFixture.emptyPatchCommand());

        verify(profiles, never()).findByEmail(any());
    }

    @Test
    @DisplayName("E-mail presente do próprio perfil não conflita")
    void sameEmailOnSameProfileIsAllowed() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new PatchProfileCommand(
                existing.id(),
                null,
                null,
                null,
                PatchValue.of(existing.email()));
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(profiles.findByEmail(existing.email())).thenReturn(Optional.of(existing));
        when(profiles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command);

        assertThat(saved.email()).isEqualTo(existing.email());
        verify(profiles).save(any());
    }

    @Test
    @DisplayName("E-mail presente livre é persistido")
    void uniquePresentEmailIsSaved() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new PatchProfileCommand(
                existing.id(),
                null,
                null,
                null,
                PatchValue.of("novo@example.com"));
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(profiles.findByEmail("novo@example.com")).thenReturn(Optional.empty());
        when(profiles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command);

        assertThat(saved.email()).isEqualTo("novo@example.com");
    }

    @Test
    @DisplayName("Nome inválido presente não consulta o repositório")
    void doesNotTouchRepositoryWhenPresentNameIsInvalid() {
        final var command = new PatchProfileCommand(
                ProfileUseCaseFixture.ID,
                PatchValue.of("123"),
                null,
                null,
                null);

        final var thrown = catchThrowable(() -> useCase.execute(command));

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

        final var saved = useCase.execute(ProfileUseCaseFixture.emptyPatchCommand());

        assertThat(saved).isEqualTo(existing);
        assertThat(saved.id()).isEqualTo(existing.id());
        assertThat(saved.name()).isEqualTo(existing.name());
        assertThat(saved.description()).isEqualTo(existing.description());
        assertThat(saved.birthday()).isEqualTo(existing.birthday());
        assertThat(saved.email()).isEqualTo(existing.email());
        assertThat(saved.password()).isEqualTo(existing.password());
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
                PatchValue.of(birthday),
                null);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(profiles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command);

        assertThat(saved.birthday()).isEqualTo(birthday);
        assertThat(saved.name()).isEqualTo(existing.name());
    }

    @Test
    @DisplayName("Id nulo: violação @NotNull no command")
    void rejectsNullId() {
        final var command = new PatchProfileCommand(null, null, null, null, null);

        final var thrown = catchThrowable(() -> useCase.execute(command));

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
                PatchValue.of("x".repeat(Description.MAX_SIZE + 1)),
                null,
                null);

        final var thrown = catchThrowable(() -> useCase.execute(command));

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
                PatchValue.of(LocalDate.now().minusYears(10)),
                null);

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findById(any());
    }

    @Test
    @DisplayName("E-mail presente inválido não consulta o repositório")
    void doesNotTouchRepositoryWhenPresentEmailIsInvalid() {
        final var command = new PatchProfileCommand(
                ProfileUseCaseFixture.ID,
                null,
                null,
                null,
                PatchValue.of("not-an-email"));

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findById(any());
        verify(profiles, never()).findByEmail(any());
    }

}
