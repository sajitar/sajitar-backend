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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sajitar.backend.application.command.checker.PatchCheckerCommand;
import com.sajitar.backend.domain.exception.CheckerNotFoundException;
import com.sajitar.backend.domain.exception.CheckerReplacesExhaustedException;
import com.sajitar.backend.domain.exception.CheckerTypeAlreadyExistsException;
import com.sajitar.backend.domain.exception.CheckerTypeRestrictedException;
import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.port.checker.CheckerRepository;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;

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
    @DisplayName("Atualiza só o payload: novo código, attempts 10 e replaces−1")
    void patchesOnlyPayload() {
        final var existing = CheckerUseCaseFixture.persistedChecker().withAttempts(3);
        when(checkers.findById(existing.id())).thenReturn(Optional.of(existing));
        when(checkers.save(any(Checker.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(new PatchCheckerCommand(existing.id(), null, "novo"));

        assertThat(saved.id()).isEqualTo(existing.id());
        assertThat(saved.profileId()).isEqualTo(existing.profileId());
        assertThat(saved.type()).isEqualTo(existing.type());
        assertThat(saved.payload()).isEqualTo("novo");
        assertThat(saved.code()).matches("^[0-9]{6}$");
        assertThat(saved.code()).isNotEqualTo(existing.code());
        assertThat(saved.attempts()).isEqualTo(Checker.ATTEMPTS_MAX);
        assertThat(saved.replaces()).isEqualTo(existing.replaces() - 1);
        verify(checkers, never()).findByProfileIdAndType(any(), any());
    }

    @Test
    @DisplayName("Atualiza só o type quando o destino está livre")
    void patchesOnlyType() {
        final var existing = CheckerUseCaseFixture.persistedChecker().withPayload("keep");
        when(checkers.findById(existing.id())).thenReturn(Optional.of(existing));
        when(checkers.findByProfileIdAndType(existing.profileId(), Checker.Type.CHANGE_PASSWORD))
                .thenReturn(Optional.empty());
        when(checkers.save(any(Checker.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(
                new PatchCheckerCommand(existing.id(), Checker.Type.CHANGE_PASSWORD, null));

        assertThat(saved.type()).isEqualTo(Checker.Type.CHANGE_PASSWORD);
        assertThat(saved.payload()).isEqualTo("keep");
        assertThat(saved.replaces()).isEqualTo(existing.replaces() - 1);
    }

    @Test
    @DisplayName("Type igual ao persistido não grava")
    void sameTypeDoesNotSave() {
        final var existing = CheckerUseCaseFixture.persistedChecker();
        when(checkers.findById(existing.id())).thenReturn(Optional.of(existing));

        final var saved = useCase.execute(new PatchCheckerCommand(existing.id(), existing.type(), null));

        assertThat(saved).isEqualTo(existing);
        verify(checkers, never()).save(any());
        verify(checkers, never()).findByProfileIdAndType(any(), any());
    }

    @Test
    @DisplayName("Payload igual ao persistido não grava")
    void samePayloadDoesNotSave() {
        final var existing = CheckerUseCaseFixture.persistedChecker().withPayload("keep");
        when(checkers.findById(existing.id())).thenReturn(Optional.of(existing));

        final var saved = useCase.execute(new PatchCheckerCommand(existing.id(), null, "keep"));

        assertThat(saved).isEqualTo(existing);
        verify(checkers, never()).save(any());
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
    @DisplayName("Troca para VERIFY_EMAIL lança 403 e não grava")
    void rejectsRestrictedTypeChange() {
        final var existing = CheckerUseCaseFixture.persistedChecker();
        when(checkers.findById(existing.id())).thenReturn(Optional.of(existing));

        final var thrown = catchThrowable(() -> useCase.execute(
                new PatchCheckerCommand(existing.id(), Checker.Type.VERIFY_EMAIL, null)));

        assertThat(thrown).isInstanceOf(CheckerTypeRestrictedException.class);
        verify(checkers, never()).findByProfileIdAndType(any(), any());
        verify(checkers, never()).save(any());
    }

    @Test
    @DisplayName("Troca para tipo já existente no perfil lança 409")
    void rejectsDuplicateType() {
        final var existing = CheckerUseCaseFixture.persistedChecker();
        when(checkers.findById(existing.id())).thenReturn(Optional.of(existing));
        when(checkers.findByProfileIdAndType(existing.profileId(), Checker.Type.CHANGE_PASSWORD))
                .thenReturn(Optional.of(CheckerUseCaseFixture.persistedChangePassword()));

        final var thrown = catchThrowable(() -> useCase.execute(
                new PatchCheckerCommand(existing.id(), Checker.Type.CHANGE_PASSWORD, "x")));

        assertThat(thrown).isInstanceOf(CheckerTypeAlreadyExistsException.class);
        verify(checkers, never()).save(any());
    }

    @Test
    @DisplayName("replaces 0 com mudança não grava")
    void rejectsWhenReplacesExhausted() {
        final var existing = CheckerUseCaseFixture.persistedChecker().withReplaces(0);
        when(checkers.findById(existing.id())).thenReturn(Optional.of(existing));

        final var thrown = catchThrowable(
                () -> useCase.execute(new PatchCheckerCommand(existing.id(), null, "novo")));

        assertThat(thrown).isInstanceOf(CheckerReplacesExhaustedException.class);
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
    @DisplayName("Id nulo: não consulta o repositório")
    void rejectsNullId() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new PatchCheckerCommand(null, null, null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotNull.class);
        verify(checkers, never()).findById(any(UUID.class));
    }

    @Test
    @DisplayName("hasChanges cobre type e payload")
    void hasChangesSemantics() {
        assertThat(CheckerUseCaseFixture.emptyPatchCommand().hasChanges()).isFalse();
        assertThat(new PatchCheckerCommand(CheckerUseCaseFixture.ID, Checker.Type.CHANGE_EMAIL, null).hasChanges())
                .isTrue();
        assertThat(new PatchCheckerCommand(CheckerUseCaseFixture.ID, null, "p").hasChanges()).isTrue();
        assertThat(new PatchCheckerCommand(CheckerUseCaseFixture.ID, Checker.Type.CHANGE_PASSWORD, "p").hasChanges())
                .isTrue();
    }

}
