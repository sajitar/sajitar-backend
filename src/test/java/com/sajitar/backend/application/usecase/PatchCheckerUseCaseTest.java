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

import com.sajitar.backend.application.command.PatchCheckerCommand;
import com.sajitar.backend.domain.exception.CheckerNotFoundException;
import com.sajitar.backend.domain.model.Checker;
import com.sajitar.backend.domain.port.CheckerRepository;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@ExtendWith(MockitoExtension.class)
@DisplayName("PatchCheckerUseCase")
class PatchCheckerUseCaseTest {

    @Mock
    private CheckerRepository checkers;

    private PatchCheckerUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new PatchCheckerUseCase(checkers, CheckerUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Atualiza só o código")
    void patchesOnlyCode() {
        final var existing = CheckerUseCaseFixture.persistedChecker().withPayload("keep");
        when(checkers.findById(existing.id())).thenReturn(Optional.of(existing));
        when(checkers.save(any(Checker.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(new PatchCheckerCommand(existing.id(), "654321", null, null, null));

        assertThat(saved.code()).isEqualTo("654321");
        assertThat(saved.payload()).isEqualTo("keep");
        assertThat(saved.attempts()).isEqualTo(existing.attempts());
        assertThat(saved.replaces()).isEqualTo(existing.replaces());
    }

    @Test
    @DisplayName("Atualiza só o payload")
    void patchesOnlyPayload() {
        final var existing = CheckerUseCaseFixture.persistedChecker();
        when(checkers.findById(existing.id())).thenReturn(Optional.of(existing));
        when(checkers.save(any(Checker.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(new PatchCheckerCommand(existing.id(), null, "novo", null, null));

        assertThat(saved.payload()).isEqualTo("novo");
        assertThat(saved.code()).isEqualTo(existing.code());
    }

    @Test
    @DisplayName("Atualiza só attempts")
    void patchesOnlyAttempts() {
        final var existing = CheckerUseCaseFixture.persistedChecker();
        when(checkers.findById(existing.id())).thenReturn(Optional.of(existing));
        when(checkers.save(any(Checker.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(new PatchCheckerCommand(existing.id(), null, null, 7, null));

        assertThat(saved.attempts()).isEqualTo(7);
        assertThat(saved.replaces()).isEqualTo(existing.replaces());
    }

    @Test
    @DisplayName("Atualiza só replaces")
    void patchesOnlyReplaces() {
        final var existing = CheckerUseCaseFixture.persistedChecker();
        when(checkers.findById(existing.id())).thenReturn(Optional.of(existing));
        when(checkers.save(any(Checker.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(new PatchCheckerCommand(existing.id(), null, null, null, 1));

        assertThat(saved.replaces()).isEqualTo(1);
        assertThat(saved.attempts()).isEqualTo(existing.attempts());
    }

    @Test
    @DisplayName("Body vazio não persiste e devolve o estado atual")
    void emptyPatchDoesNotSave() {
        final var existing = CheckerUseCaseFixture.persistedChecker();
        when(checkers.findById(existing.id())).thenReturn(Optional.of(existing));

        final var saved = useCase.execute(CheckerUseCaseFixture.emptyPatchCommand());

        assertThat(saved).isEqualTo(existing);
        assertThat(saved.updatedAt()).isEqualTo(existing.updatedAt());
        verify(checkers, never()).save(any());
    }

    @Test
    @DisplayName("Lança CheckerNotFoundException quando o id não existe")
    void throwsWhenMissing() {
        when(checkers.findById(CheckerUseCaseFixture.ID)).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(CheckerUseCaseFixture.emptyPatchCommand()));

        assertThat(thrown).isInstanceOf(CheckerNotFoundException.class);
        verify(checkers, never()).save(any());
    }

    @Test
    @DisplayName("Código inválido: não consulta o repositório")
    void rejectsInvalidCode() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new PatchCheckerCommand(CheckerUseCaseFixture.ID, "12", null, null, null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(Pattern.class);
        verify(checkers, never()).findById(any());
    }

    @Test
    @DisplayName("attempts inválido: não consulta o repositório")
    void rejectsInvalidAttempts() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new PatchCheckerCommand(CheckerUseCaseFixture.ID, null, null, -1, null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(checkers, never()).findById(any());
    }

    @Test
    @DisplayName("replaces inválido: não consulta o repositório")
    void rejectsInvalidReplaces() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new PatchCheckerCommand(CheckerUseCaseFixture.ID, null, null, null, 9)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(checkers, never()).findById(any());
    }

    @Test
    @DisplayName("Id nulo: não consulta o repositório")
    void rejectsNullId() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new PatchCheckerCommand(null, null, null, null, null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotNull.class);
        verify(checkers, never()).findById(any(UUID.class));
    }

    @Test
    @DisplayName("hasChanges cobre cada campo mutável")
    void hasChangesSemantics() {
        assertThat(CheckerUseCaseFixture.emptyPatchCommand().hasChanges()).isFalse();
        assertThat(new PatchCheckerCommand(CheckerUseCaseFixture.ID, "123456", null, null, null).hasChanges()).isTrue();
        assertThat(new PatchCheckerCommand(CheckerUseCaseFixture.ID, null, "p", null, null).hasChanges()).isTrue();
        assertThat(new PatchCheckerCommand(CheckerUseCaseFixture.ID, null, null, 1, null).hasChanges()).isTrue();
        assertThat(new PatchCheckerCommand(CheckerUseCaseFixture.ID, null, null, null, 1).hasChanges()).isTrue();
    }

}
