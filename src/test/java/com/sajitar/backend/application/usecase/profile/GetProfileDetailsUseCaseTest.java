package com.sajitar.backend.application.usecase.profile;

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

import com.sajitar.backend.domain.exception.ForbiddenProfileDetailsException;
import com.sajitar.backend.domain.model.profile.Profile;
import com.sajitar.backend.domain.port.profile.ProfileRepository;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetProfileDetailsUseCase")
class GetProfileDetailsUseCaseTest {

    private static final UUID VIEWER = UUID.fromString("550e8400-e29b-41d4-a716-446655440099");

    @Mock
    private ProfileRepository profiles;

    private GetProfileDetailsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetProfileDetailsUseCase(profiles, ProfileUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Ausente: não consulta o viewer")
    void missingDoesNotConsultViewer() {
        final var id = ProfileUseCaseFixture.ID;
        when(profiles.findById(id)).thenReturn(Optional.empty());

        assertThat(useCase.execute(id, VIEWER)).isEmpty();
        verify(profiles).findById(id);
        verify(profiles, never()).findById(VIEWER);
    }

    @Test
    @DisplayName("Dono vê o próprio perfil")
    void ownerSeesOwnProfile() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        when(profiles.findById(profile.id())).thenReturn(Optional.of(profile));

        assertThat(useCase.execute(profile.id(), profile.id())).contains(profile);
        verify(profiles).findById(profile.id());
        verify(profiles, never()).findById(VIEWER);
    }

    @Test
    @DisplayName("Dono vê o próprio perfil mesmo sem ser MASTER")
    void ownerSeesOwnProfileWithoutBeingMaster() {
        final var profile = ProfileUseCaseFixture.persistedProfile().withType(Profile.Type.READER);
        when(profiles.findById(profile.id())).thenReturn(Optional.of(profile));

        assertThat(useCase.execute(profile.id(), profile.id())).contains(profile);
    }

    @Test
    @DisplayName("MASTER vê detalhes de outro perfil")
    void masterSeesAnotherProfile() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        when(profiles.findById(profile.id())).thenReturn(Optional.of(profile));
        when(profiles.findById(VIEWER)).thenReturn(Optional.of(ProfileUseCaseFixture.persistedMaster(VIEWER)));

        assertThat(useCase.execute(profile.id(), VIEWER)).contains(profile);
    }

    @Test
    @DisplayName("Não-MASTER não vê detalhes de outro perfil")
    void nonMasterCannotSeeAnotherProfile() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        when(profiles.findById(profile.id())).thenReturn(Optional.of(profile));
        when(profiles.findById(VIEWER))
                .thenReturn(Optional.of(profile.withId(VIEWER).withType(Profile.Type.READER)));

        final var thrown = catchThrowable(() -> useCase.execute(profile.id(), VIEWER));

        assertThat(thrown).isInstanceOf(ForbiddenProfileDetailsException.class);
        assertThat(((ForbiddenProfileDetailsException) thrown).content().get("id"))
                .containsExactly(ForbiddenProfileDetailsException.MESSAGE_KEY);
    }

    @Test
    @DisplayName("Viewer inexistente não vê detalhes de outro perfil")
    void missingViewerCannotSeeAnotherProfile() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        when(profiles.findById(profile.id())).thenReturn(Optional.of(profile));
        when(profiles.findById(VIEWER)).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(profile.id(), VIEWER));

        assertThat(thrown).isInstanceOf(ForbiddenProfileDetailsException.class);
    }

    @Test
    @DisplayName("Id nulo: não chama o repositório")
    void doesNotCallRepositoryWhenIdIsNull() {
        final var thrown = catchThrowable(() -> useCase.execute(null, VIEWER));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotNull.class);
        verify(profiles, never()).findById(any(UUID.class));
    }

    @Test
    @DisplayName("Viewer nulo: não chama o repositório")
    void doesNotCallRepositoryWhenViewerIsNull() {
        final var thrown = catchThrowable(() -> useCase.execute(ProfileUseCaseFixture.ID, null));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findById(any(UUID.class));
    }

}
