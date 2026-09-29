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

import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.model.profile.Profile;
import com.sajitar.backend.domain.port.checker.CheckerRepository;
import com.sajitar.backend.domain.port.profile.ProfileRepository;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetProfileUseCase")
class GetProfileUseCaseTest {

    private static final UUID VIEWER = UUID.fromString("550e8400-e29b-41d4-a716-446655440099");

    @Mock
    private ProfileRepository profiles;

    @Mock
    private CheckerRepository checkers;

    private GetProfileUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetProfileUseCase(profiles, checkers, ProfileUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Ausente: não consulta o viewer nem o checker")
    void missingDoesNotConsultViewerOrChecker() {
        final var id = ProfileUseCaseFixture.ID;
        when(profiles.findById(id)).thenReturn(Optional.empty());

        assertThat(useCase.execute(id, VIEWER)).isEmpty();
        verify(profiles).findById(id);
        verify(checkers, never()).findByProfileIdAndType(any(), any());
    }

    @Test
    @DisplayName("MASTER vê perfil com VERIFY_EMAIL e não consulta checker")
    void masterSeesUnverifiedWithoutCheckingChecker() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        when(profiles.findById(profile.id())).thenReturn(Optional.of(profile));
        when(profiles.findById(VIEWER)).thenReturn(Optional.of(ProfileUseCaseFixture.persistedMaster(VIEWER)));

        assertThat(useCase.execute(profile.id(), VIEWER)).contains(profile);
        verify(checkers, never()).findByProfileIdAndType(any(), any());
    }

    @Test
    @DisplayName("MASTER vê READER")
    void masterSeesReader() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        when(profiles.findById(profile.id())).thenReturn(Optional.of(profile));
        when(profiles.findById(VIEWER)).thenReturn(Optional.of(ProfileUseCaseFixture.persistedMaster(VIEWER)));

        assertThat(useCase.execute(profile.id(), VIEWER)).contains(profile);
        verify(checkers, never()).findByProfileIdAndType(any(), any());
    }

    @Test
    @DisplayName("WRITER não vê perfil WRITER com VERIFY_EMAIL")
    void writerDoesNotSeeUnverified() {
        final var profile = ProfileUseCaseFixture.persistedProfile().withType(Profile.Type.WRITER);
        when(profiles.findById(profile.id())).thenReturn(Optional.of(profile));
        when(profiles.findById(VIEWER)).thenReturn(Optional.of(profile.withId(VIEWER)));
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL))
                .thenReturn(Optional.of(Checker.create(profile.id(), Checker.Type.VERIFY_EMAIL)));

        assertThat(useCase.execute(profile.id(), VIEWER)).isEmpty();
    }

    @Test
    @DisplayName("READER não vê perfil WRITER com VERIFY_EMAIL")
    void readerDoesNotSeeUnverified() {
        final var profile = ProfileUseCaseFixture.persistedProfile().withType(Profile.Type.WRITER);
        when(profiles.findById(profile.id())).thenReturn(Optional.of(profile));
        when(profiles.findById(VIEWER)).thenReturn(Optional.of(
                ProfileUseCaseFixture.persistedProfile().withId(VIEWER)));
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL))
                .thenReturn(Optional.of(Checker.create(profile.id(), Checker.Type.VERIFY_EMAIL)));

        assertThat(useCase.execute(profile.id(), VIEWER)).isEmpty();
    }

    @Test
    @DisplayName("Não-MASTER vê perfil WRITER verificado")
    void nonMasterSeesVerified() {
        final var profile = ProfileUseCaseFixture.persistedProfile().withType(Profile.Type.WRITER);
        when(profiles.findById(profile.id())).thenReturn(Optional.of(profile));
        when(profiles.findById(VIEWER)).thenReturn(Optional.of(profile.withId(VIEWER)));
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL)).thenReturn(Optional.empty());

        assertThat(useCase.execute(profile.id(), VIEWER)).contains(profile);
    }

    @Test
    @DisplayName("Não-MASTER não vê outro READER e não consulta checker")
    void nonMasterDoesNotSeeOtherReader() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        when(profiles.findById(profile.id())).thenReturn(Optional.of(profile));
        when(profiles.findById(VIEWER)).thenReturn(Optional.of(
                profile.withId(VIEWER).withType(Profile.Type.WRITER)));

        assertThat(useCase.execute(profile.id(), VIEWER)).isEmpty();
        verify(checkers, never()).findByProfileIdAndType(any(), any());
    }

    @Test
    @DisplayName("READER vê o próprio perfil verificado")
    void readerSeesOwnVerified() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        when(profiles.findById(profile.id())).thenReturn(Optional.of(profile));
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL)).thenReturn(Optional.empty());

        assertThat(useCase.execute(profile.id(), profile.id())).contains(profile);
    }

    @Test
    @DisplayName("Dono READER com VERIFY_EMAIL continua oculto")
    void ownerReaderWithVerifyEmailRemainsHidden() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        when(profiles.findById(profile.id())).thenReturn(Optional.of(profile));
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL))
                .thenReturn(Optional.of(Checker.create(profile.id(), Checker.Type.VERIFY_EMAIL)));

        assertThat(useCase.execute(profile.id(), profile.id())).isEmpty();
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
