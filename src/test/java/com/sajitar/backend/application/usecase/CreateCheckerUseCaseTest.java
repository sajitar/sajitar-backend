package com.sajitar.backend.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sajitar.backend.application.command.CreateCheckerCommand;
import com.sajitar.backend.domain.exception.CheckerTypeAlreadyExistsException;
import com.sajitar.backend.domain.exception.CheckerTypeRestrictedException;
import com.sajitar.backend.domain.exception.ProfileUnavailableException;
import com.sajitar.backend.domain.model.Checker;
import com.sajitar.backend.domain.port.CheckerRepository;
import com.sajitar.backend.domain.port.ProfileRepository;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateCheckerUseCase")
class CreateCheckerUseCaseTest {

    @Mock
    private CheckerRepository checkers;

    @Mock
    private ProfileRepository profiles;

    private CreateCheckerUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateCheckerUseCase(checkers, profiles, CheckerUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Persiste CHANGE_EMAIL com código gerado e defaults")
    void persistsChangeEmailWithDefaults() {
        final var command = CheckerUseCaseFixture.validCreateCommand();
        when(profiles.findById(command.profileId())).thenReturn(Optional.of(ProfileUseCaseFixture.persistedProfile()));
        when(checkers.findByProfileIdAndType(command.profileId(), command.type())).thenReturn(Optional.empty());
        when(checkers.save(any(Checker.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command);

        assertThat(saved.type()).isEqualTo(Checker.Type.CHANGE_EMAIL);
        assertThat(saved.code()).matches("^[0-9]{6}$");
        assertThat(saved.attempts()).isEqualTo(Checker.ATTEMPTS_MAX);
        assertThat(saved.replaces()).isEqualTo(Checker.REPLACES_MAX);
        assertThat(saved.payload()).isNull();
        final var captor = ArgumentCaptor.forClass(Checker.class);
        verify(checkers).save(captor.capture());
        assertThat(captor.getValue().profileId()).isEqualTo(command.profileId());
    }

    @Test
    @DisplayName("Persiste CHANGE_PASSWORD quando o tipo está livre")
    void persistsChangePassword() {
        final var command = new CreateCheckerCommand(CheckerUseCaseFixture.PROFILE_ID, Checker.Type.CHANGE_PASSWORD);
        when(profiles.findById(command.profileId())).thenReturn(Optional.of(ProfileUseCaseFixture.persistedProfile()));
        when(checkers.findByProfileIdAndType(command.profileId(), command.type())).thenReturn(Optional.empty());
        when(checkers.save(any(Checker.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command);

        assertThat(saved.type()).isEqualTo(Checker.Type.CHANGE_PASSWORD);
        assertThat(saved.requiredPayload()).isTrue();
    }

    @Test
    @DisplayName("VERIFY_EMAIL: 403 e não consulta repositórios")
    void rejectsRestrictedType() {
        final var command = new CreateCheckerCommand(CheckerUseCaseFixture.PROFILE_ID, Checker.Type.VERIFY_EMAIL);

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(CheckerTypeRestrictedException.class);
        assertThat(((CheckerTypeRestrictedException) thrown).content().get("type"))
                .containsExactly(CheckerTypeRestrictedException.CREATE_KEY);
        verify(profiles, never()).findById(any());
        verify(checkers, never()).findByProfileIdAndType(any(), any());
        verify(checkers, never()).save(any());
    }

    @Test
    @DisplayName("Perfil inexistente lança ProfileUnavailableException")
    void throwsWhenProfileIsMissing() {
        final var command = CheckerUseCaseFixture.validCreateCommand();
        when(profiles.findById(command.profileId())).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(ProfileUnavailableException.class);
        verify(checkers, never()).findByProfileIdAndType(any(), any());
        verify(checkers, never()).save(any());
    }

    @Test
    @DisplayName("Tipo duplicado lança CheckerTypeAlreadyExistsException")
    void throwsWhenTypeAlreadyExists() {
        final var command = CheckerUseCaseFixture.validCreateCommand();
        when(profiles.findById(command.profileId())).thenReturn(Optional.of(ProfileUseCaseFixture.persistedProfile()));
        when(checkers.findByProfileIdAndType(command.profileId(), command.type()))
                .thenReturn(Optional.of(CheckerUseCaseFixture.persistedChecker()));

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(CheckerTypeAlreadyExistsException.class);
        assertThat(((CheckerTypeAlreadyExistsException) thrown).content().get("type"))
                .containsExactly(CheckerTypeAlreadyExistsException.MESSAGE_KEY);
        verify(checkers, never()).save(any());
        verifyNoMoreInteractions(checkers);
    }

    @Test
    @DisplayName("profileId nulo: não consulta repositórios")
    void rejectsNullProfileId() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new CreateCheckerCommand(null, Checker.Type.CHANGE_EMAIL)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotNull.class);
        verify(profiles, never()).findById(any());
        verify(checkers, never()).save(any());
    }

    @Test
    @DisplayName("type nulo: não consulta repositórios")
    void rejectsNullType() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new CreateCheckerCommand(CheckerUseCaseFixture.PROFILE_ID, null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findById(any());
        verify(checkers, never()).save(any());
    }

}
