package com.sajitar.backend.application.usecase;

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

import com.sajitar.backend.application.command.DeleteCheckerCommand;
import com.sajitar.backend.domain.exception.CheckerNotFoundException;
import com.sajitar.backend.domain.exception.CheckerTypeRestrictedException;
import com.sajitar.backend.domain.port.CheckerRepository;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteCheckerUseCase")
class DeleteCheckerUseCaseTest {

    @Mock
    private CheckerRepository checkers;

    private DeleteCheckerUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteCheckerUseCase(checkers, CheckerUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Remove o checker quando o tipo não é restrito")
    void deletesWhenNotRestricted() {
        final var existing = CheckerUseCaseFixture.persistedChecker();
        when(checkers.findById(existing.id())).thenReturn(Optional.of(existing));

        useCase.execute(new DeleteCheckerCommand(existing.id()));

        verify(checkers).deleteById(existing.id());
    }

    @Test
    @DisplayName("Lança CheckerNotFoundException quando o id não existe")
    void throwsWhenMissing() {
        when(checkers.findById(CheckerUseCaseFixture.ID)).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(new DeleteCheckerCommand(CheckerUseCaseFixture.ID)));

        assertThat(thrown).isInstanceOf(CheckerNotFoundException.class);
        verify(checkers, never()).deleteById(any());
    }

    @Test
    @DisplayName("VERIFY_EMAIL: 403 e não exclui")
    void rejectsRestrictedType() {
        final var existing = CheckerUseCaseFixture.persistedVerifyEmail();
        when(checkers.findById(existing.id())).thenReturn(Optional.of(existing));

        final var thrown = catchThrowable(() -> useCase.execute(new DeleteCheckerCommand(existing.id())));

        assertThat(thrown).isInstanceOf(CheckerTypeRestrictedException.class);
        assertThat(((CheckerTypeRestrictedException) thrown).content().get("type"))
                .containsExactly(CheckerTypeRestrictedException.DELETE_KEY);
        verify(checkers, never()).deleteById(any());
    }

    @Test
    @DisplayName("Id nulo: não consulta o repositório")
    void rejectsNullId() {
        final var thrown = catchThrowable(() -> useCase.execute(new DeleteCheckerCommand(null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotNull.class);
        verify(checkers, never()).findById(any(UUID.class));
    }

}
