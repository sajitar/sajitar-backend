package com.sajitar.backend.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

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

import com.sajitar.backend.application.command.UpdateProfileCommand;
import com.sajitar.backend.domain.exception.EmailAlreadyRegisteredException;
import com.sajitar.backend.domain.exception.ProfileNotFoundException;
import com.sajitar.backend.domain.model.Profile;
import com.sajitar.backend.domain.port.PasswordHasher;
import com.sajitar.backend.domain.port.ProfileRepository;
import com.sajitar.backend.domain.validation.Limit;
import com.sajitar.backend.domain.validation.profile.Birthday;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.Size;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateProfileUseCase")
class UpdateProfileUseCaseTest {

    @Mock
    private ProfileRepository profiles;

    @Mock
    private PasswordHasher passwordHasher;

    private UpdateProfileUseCase useCase;

    @BeforeAll
    static void configureValidation() {
        Birthday.BirthdayValidator.configure(18);
        Limit.LimitValidator.configure(100);
    }

    @BeforeEach
    void setUp() {
        useCase = new UpdateProfileUseCase(profiles, passwordHasher, ProfileUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Mantém a senha atual quando o command não informa senha nova")
    void keepsExistingPasswordWhenBlank() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = ProfileUseCaseFixture.validUpdateCommand();
        when(profiles.findById(command.id())).thenReturn(Optional.of(existing));
        when(profiles.findByEmail(command.email())).thenReturn(Optional.of(existing));
        when(profiles.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command);

        assertThat(saved.password()).isEqualTo(existing.password());
        verify(passwordHasher, never()).hash(any());
        verify(profiles).save(any(Profile.class));
    }

    @Test
    @DisplayName("Recodifica a senha quando uma senha nova é informada")
    void hashesWhenNewPasswordIsPresent() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new UpdateProfileCommand(
                existing.id(),
                existing.name(),
                existing.description(),
                existing.birthday(),
                existing.email(),
                "novaSenhaSegura");
        when(profiles.findById(command.id())).thenReturn(Optional.of(existing));
        when(profiles.findByEmail(command.email())).thenReturn(Optional.of(existing));
        when(passwordHasher.hash("novaSenhaSegura")).thenReturn("$2a$new");
        when(profiles.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command);

        assertThat(saved.password()).isEqualTo("$2a$new");
        verify(passwordHasher).hash("novaSenhaSegura");
    }

    @Test
    @DisplayName("Lança ProfileNotFoundException quando o id não existe")
    void throwsWhenProfileDoesNotExist() {
        final var command = ProfileUseCaseFixture.validUpdateCommand();
        when(profiles.findById(command.id())).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(ProfileNotFoundException.class);
        verify(profiles).findById(command.id());
        verify(profiles, never()).save(any());
        verify(passwordHasher, never()).hash(any());
    }

    @Test
    @DisplayName("Lança EmailAlreadyRegisteredException quando o e-mail pertence a outro perfil")
    void throwsWhenEmailBelongsToAnotherProfile() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var other = existing.withId(UUID.randomUUID());
        final var command = ProfileUseCaseFixture.validUpdateCommand();
        when(profiles.findById(command.id())).thenReturn(Optional.of(existing));
        when(profiles.findByEmail(command.email())).thenReturn(Optional.of(other));

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(EmailAlreadyRegisteredException.class);
        assertThat(((EmailAlreadyRegisteredException) thrown).content().get("email"))
                .containsExactly(EmailAlreadyRegisteredException.MESSAGE_KEY);
        verify(profiles, never()).save(any());
        verifyNoMoreInteractions(passwordHasher);
    }

    @Test
    @DisplayName("Persiste ao atualizar o mesmo perfil (mesmo id e e-mail)")
    void persistsWhenUpdatingSameProfile() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new UpdateProfileCommand(
                existing.id(),
                "Nome Atualizado",
                existing.description(),
                existing.birthday(),
                existing.email(),
                null);
        when(profiles.findById(command.id())).thenReturn(Optional.of(existing));
        when(profiles.findByEmail(command.email())).thenReturn(Optional.of(existing));
        when(profiles.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command);

        assertThat(saved.name()).isEqualTo("Nome Atualizado");
        final var captor = ArgumentCaptor.forClass(Profile.class);
        verify(profiles).save(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo(existing.id());
        assertThat(captor.getValue().password()).isEqualTo(existing.password());
    }

    @Test
    @DisplayName("Senha só com espaços é tratada como ausência de senha nova")
    void blankPasswordIsNotANewPassword() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new UpdateProfileCommand(
                existing.id(),
                existing.name(),
                existing.description(),
                existing.birthday(),
                existing.email(),
                "   ");
        when(profiles.findById(command.id())).thenReturn(Optional.of(existing));
        when(profiles.findByEmail(command.email())).thenReturn(Optional.of(existing));
        when(profiles.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command);

        assertThat(saved.password()).isEqualTo(existing.password());
        verify(passwordHasher, never()).hash(any());
    }

    @Test
    @DisplayName("Senha nova inválida: não consulta o repositório")
    void doesNotTouchRepositoryWhenNewPasswordIsInvalid() {
        final var command = new UpdateProfileCommand(
                ProfileUseCaseFixture.ID,
                ProfileUseCaseFixture.NAME,
                ProfileUseCaseFixture.DESCRIPTION,
                ProfileUseCaseFixture.BIRTHDAY,
                ProfileUseCaseFixture.EMAIL,
                "1234567");

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(Size.class);
        verify(profiles, never()).findById(any());
        verify(passwordHasher, never()).hash(any());
    }

}
