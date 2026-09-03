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

import com.sajitar.backend.domain.port.profile.ProfileRepository;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetProfileUseCase")
class GetProfileUseCaseTest {

    @Mock
    private ProfileRepository profiles;

    private GetProfileUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetProfileUseCase(profiles, ProfileUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Delega ao repositório quando o id é válido")
    void delegatesToRepositoryWhenIdIsValid() {
        final var id = ProfileUseCaseFixture.ID;
        when(profiles.findById(id)).thenReturn(Optional.empty());

        final var result = useCase.execute(id);

        assertThat(result).isEmpty();
        verify(profiles).findById(id);
    }

    @Test
    @DisplayName("Retorna o perfil quando encontrado")
    void returnsProfileWhenFound() {
        final var profile = ProfileUseCaseFixture.persistedProfile();
        when(profiles.findById(profile.id())).thenReturn(Optional.of(profile));

        assertThat(useCase.execute(profile.id())).contains(profile);
    }

    @Test
    @DisplayName("Id nulo: não chama o repositório")
    void doesNotCallRepositoryWhenIdIsNull() {
        final var thrown = catchThrowable(() -> useCase.execute(null));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotNull.class);
        verify(profiles, never()).findById(any(UUID.class));
    }

}
