package com.sajitar.backend.application.usecase.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sajitar.backend.application.command.profile.UpdateProfileCommand;
import com.sajitar.backend.domain.exception.ProfileNotFoundException;
import com.sajitar.backend.domain.model.profile.Profile;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.validation.Limit;
import com.sajitar.backend.domain.validation.profile.Birthday;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateProfileUseCase")
class UpdateProfileUseCaseTest {

    @Mock
    private ProfileRepository profiles;

    private UpdateProfileUseCase useCase;

    @BeforeAll
    static void configureValidation() {
        Birthday.BirthdayValidator.configure(18);
        Limit.LimitValidator.configure(100);
    }

    @BeforeEach
    void setUp() {
        useCase = new UpdateProfileUseCase(profiles, ProfileUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Mantém o hash da senha e o e-mail ao atualizar os demais campos")
    void keepsExistingPasswordAndEmail() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = ProfileUseCaseFixture.validUpdateCommand();
        when(profiles.findById(command.id())).thenReturn(Optional.of(existing));
        when(profiles.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command);

        assertThat(saved.password()).isEqualTo(existing.password());
        assertThat(saved.email()).isEqualTo(existing.email());
        assertThat(saved.type()).isEqualTo(existing.type());
        verify(profiles).save(any(Profile.class));
        verify(profiles, never()).findByEmail(any());
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
    }

    @Test
    @DisplayName("Persiste o nome novo mantendo id, senha e e-mail")
    void persistsWhenUpdatingSameProfile() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new UpdateProfileCommand(
                existing.id(),
                existing.type(),
                "Nome Atualizado",
                existing.description(),
                existing.birthday());
        when(profiles.findById(command.id())).thenReturn(Optional.of(existing));
        when(profiles.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command);

        assertThat(saved.name()).isEqualTo("Nome Atualizado");
        final var captor = ArgumentCaptor.forClass(Profile.class);
        verify(profiles).save(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo(existing.id());
        assertThat(captor.getValue().password()).isEqualTo(existing.password());
        assertThat(captor.getValue().email()).isEqualTo(existing.email());
    }

    @Test
    @DisplayName("Substitui o tipo vigente")
    void replacesType() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new UpdateProfileCommand(
                existing.id(),
                Profile.Type.MASTER,
                existing.name(),
                existing.description(),
                existing.birthday());
        when(profiles.findById(command.id())).thenReturn(Optional.of(existing));
        when(profiles.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command);

        assertThat(saved.type()).isEqualTo(Profile.Type.MASTER);
        assertThat(saved.name()).isEqualTo(existing.name());
    }

    @Test
    @DisplayName("Tipo nulo: não consulta o repositório")
    void doesNotTouchRepositoryWhenTypeIsNull() {
        final var command = new UpdateProfileCommand(
                ProfileUseCaseFixture.ID,
                null,
                ProfileUseCaseFixture.NAME,
                ProfileUseCaseFixture.DESCRIPTION,
                ProfileUseCaseFixture.BIRTHDAY);

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(jakarta.validation.ConstraintViolationException.class);
        verify(profiles, never()).findById(any());
        verify(profiles, never()).save(any());
    }

}
