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

import com.sajitar.backend.domain.model.authority.Authority;
import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.port.authority.AuthorityRepository;
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
    private AuthorityRepository authorities;

    @Mock
    private CheckerRepository checkers;

    private GetProfileUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetProfileUseCase(profiles, authorities, checkers, ProfileUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Ausente: não consulta authority nem checker")
    void missingDoesNotConsultAuthorityOrChecker() {
        final var id = ProfileUseCaseFixture.ID;
        when(profiles.findById(id)).thenReturn(Optional.empty());

        assertThat(useCase.execute(id, VIEWER)).isEmpty();
        verify(authorities, never()).findByProfileIdAndType(any(), any());
        verify(checkers, never()).findByProfileIdAndType(any(), any());
    }

    @Test
    @DisplayName("MASTER vê perfil com VERIFY_EMAIL e não consulta checker")
    void masterSeesUnverifiedWithoutCheckingChecker() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        when(profiles.findById(profile.id())).thenReturn(Optional.of(profile));
        when(authorities.findByProfileIdAndType(VIEWER, Authority.Type.MASTER))
                .thenReturn(Optional.of(Authority.create(VIEWER, Authority.Type.MASTER)));

        assertThat(useCase.execute(profile.id(), VIEWER)).contains(profile);
        verify(checkers, never()).findByProfileIdAndType(any(), any());
    }

    @Test
    @DisplayName("Não-MASTER não vê perfil com VERIFY_EMAIL")
    void nonMasterDoesNotSeeUnverified() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        when(profiles.findById(profile.id())).thenReturn(Optional.of(profile));
        when(authorities.findByProfileIdAndType(VIEWER, Authority.Type.MASTER)).thenReturn(Optional.empty());
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL))
                .thenReturn(Optional.of(Checker.create(profile.id(), Checker.Type.VERIFY_EMAIL)));

        assertThat(useCase.execute(profile.id(), VIEWER)).isEmpty();
    }

    @Test
    @DisplayName("Não-MASTER vê perfil verificado")
    void nonMasterSeesVerified() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        when(profiles.findById(profile.id())).thenReturn(Optional.of(profile));
        when(authorities.findByProfileIdAndType(VIEWER, Authority.Type.MASTER)).thenReturn(Optional.empty());
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL)).thenReturn(Optional.empty());

        assertThat(useCase.execute(profile.id(), VIEWER)).contains(profile);
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
