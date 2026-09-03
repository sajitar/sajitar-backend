package com.sajitar.backend.application.usecase.checker;

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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sajitar.backend.application.command.checker.UpdateCheckerCommand;
import com.sajitar.backend.domain.exception.CheckerNotFoundException;
import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.port.checker.CheckerRepository;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateCheckerUseCase")
class UpdateCheckerUseCaseTest {

    @Mock
    private CheckerRepository checkers;

    private UpdateCheckerUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateCheckerUseCase(checkers, CheckerUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Campos omitidos voltam aos defaults de criação e geram código novo")
    void omittedFieldsResetToDefaults() {
        final var existing = CheckerUseCaseFixture.persistedChecker().withPayload("old").withAttempts(2).withReplaces(1);
        when(checkers.findById(existing.id())).thenReturn(Optional.of(existing));
        when(checkers.save(any(Checker.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(CheckerUseCaseFixture.emptyUpdateCommand());

        assertThat(saved.id()).isEqualTo(existing.id());
        assertThat(saved.profileId()).isEqualTo(existing.profileId());
        assertThat(saved.type()).isEqualTo(existing.type());
        assertThat(saved.code()).matches("^[0-9]{6}$");
        assertThat(saved.payload()).isNull();
        assertThat(saved.attempts()).isEqualTo(Checker.ATTEMPTS_MAX);
        assertThat(saved.replaces()).isEqualTo(Checker.REPLACES_MAX);
        assertThat(saved.updatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Campos presentes substituem os valores")
    void presentFieldsAreApplied() {
        final var existing = CheckerUseCaseFixture.persistedChecker();
        when(checkers.findById(existing.id())).thenReturn(Optional.of(existing));
        when(checkers.save(any(Checker.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(new UpdateCheckerCommand(existing.id(), "654321", "novo", 4, 1));

        assertThat(saved.code()).isEqualTo("654321");
        assertThat(saved.payload()).isEqualTo("novo");
        assertThat(saved.attempts()).isEqualTo(4);
        assertThat(saved.replaces()).isEqualTo(1);
        final var captor = ArgumentCaptor.forClass(Checker.class);
        verify(checkers).save(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo(existing.id());
    }

    @Test
    @DisplayName("Lança CheckerNotFoundException quando o id não existe")
    void throwsWhenMissing() {
        when(checkers.findById(CheckerUseCaseFixture.ID)).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(CheckerUseCaseFixture.emptyUpdateCommand()));

        assertThat(thrown).isInstanceOf(CheckerNotFoundException.class);
        verify(checkers, never()).save(any());
    }

    @Test
    @DisplayName("Código inválido: não consulta o repositório")
    void rejectsInvalidCode() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new UpdateCheckerCommand(CheckerUseCaseFixture.ID, "abc", null, null, null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(Pattern.class);
        verify(checkers, never()).findById(any());
    }

    @Test
    @DisplayName("attempts inválido: não consulta o repositório")
    void rejectsInvalidAttempts() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new UpdateCheckerCommand(CheckerUseCaseFixture.ID, null, null, 11, null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(checkers, never()).findById(any());
    }

    @Test
    @DisplayName("replaces inválido: não consulta o repositório")
    void rejectsInvalidReplaces() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new UpdateCheckerCommand(CheckerUseCaseFixture.ID, null, null, null, 4)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(checkers, never()).findById(any());
    }

    @Test
    @DisplayName("Id nulo: não consulta o repositório")
    void rejectsNullId() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new UpdateCheckerCommand(null, null, null, null, null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotNull.class);
        verify(checkers, never()).findById(any(UUID.class));
    }

}
