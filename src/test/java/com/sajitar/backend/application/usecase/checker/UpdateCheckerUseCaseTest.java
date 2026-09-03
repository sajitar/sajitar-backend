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
import com.sajitar.backend.domain.exception.CheckerReplacesExhaustedException;
import com.sajitar.backend.domain.exception.CheckerTypeAlreadyExistsException;
import com.sajitar.backend.domain.exception.CheckerTypeRestrictedException;
import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.port.checker.CheckerRepository;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;

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
    @DisplayName("Altera payload: novo código, attempts 10, replaces−1 e id/profileId iguais")
    void appliesPayloadChange() {
        final var existing = CheckerUseCaseFixture.persistedChecker().withAttempts(2).withReplaces(2);
        when(checkers.findById(existing.id())).thenReturn(Optional.of(existing));
        when(checkers.save(any(Checker.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(new UpdateCheckerCommand(existing.id(), existing.type(), "novo"));

        assertThat(saved.id()).isEqualTo(existing.id());
        assertThat(saved.profileId()).isEqualTo(existing.profileId());
        assertThat(saved.type()).isEqualTo(existing.type());
        assertThat(saved.payload()).isEqualTo("novo");
        assertThat(saved.code()).matches("^[0-9]{6}$");
        assertThat(saved.code()).isNotEqualTo(existing.code());
        assertThat(saved.attempts()).isEqualTo(Checker.ATTEMPTS_MAX);
        assertThat(saved.replaces()).isEqualTo(1);
        assertThat(saved.updatedAt()).isAfter(existing.updatedAt());
        final var captor = ArgumentCaptor.forClass(Checker.class);
        verify(checkers).save(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo(existing.id());
        verify(checkers, never()).findByProfileIdAndType(any(), any());
    }

    @Test
    @DisplayName("Valores idênticos não persistem nem consomem replace")
    void identicalValuesDoNotSave() {
        final var existing = CheckerUseCaseFixture.persistedChecker();
        when(checkers.findById(existing.id())).thenReturn(Optional.of(existing));

        final var saved = useCase.execute(CheckerUseCaseFixture.identicalUpdateCommand());

        assertThat(saved).isEqualTo(existing);
        assertThat(saved.code()).isEqualTo(existing.code());
        assertThat(saved.replaces()).isEqualTo(existing.replaces());
        verify(checkers, never()).save(any());
        verify(checkers, never()).findByProfileIdAndType(any(), any());
    }

    @Test
    @DisplayName("Troca de tipo livre persiste com o type novo")
    void appliesTypeChange() {
        final var existing = CheckerUseCaseFixture.persistedChecker();
        when(checkers.findById(existing.id())).thenReturn(Optional.of(existing));
        when(checkers.findByProfileIdAndType(existing.profileId(), Checker.Type.CHANGE_PASSWORD))
                .thenReturn(Optional.empty());
        when(checkers.save(any(Checker.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(
                new UpdateCheckerCommand(existing.id(), Checker.Type.CHANGE_PASSWORD, existing.payload()));

        assertThat(saved.type()).isEqualTo(Checker.Type.CHANGE_PASSWORD);
        assertThat(saved.replaces()).isEqualTo(existing.replaces() - 1);
        verify(checkers).findByProfileIdAndType(existing.profileId(), Checker.Type.CHANGE_PASSWORD);
    }

    @Test
    @DisplayName("Troca para VERIFY_EMAIL lança 403 e não grava")
    void rejectsRestrictedTypeChange() {
        final var existing = CheckerUseCaseFixture.persistedChecker();
        when(checkers.findById(existing.id())).thenReturn(Optional.of(existing));

        final var thrown = catchThrowable(() -> useCase.execute(
                new UpdateCheckerCommand(existing.id(), Checker.Type.VERIFY_EMAIL, null)));

        assertThat(thrown).isInstanceOf(CheckerTypeRestrictedException.class);
        assertThat(((CheckerTypeRestrictedException) thrown).content().get("type"))
                .containsExactly(CheckerTypeRestrictedException.CREATE_KEY);
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
                new UpdateCheckerCommand(existing.id(), Checker.Type.CHANGE_PASSWORD, null)));

        assertThat(thrown).isInstanceOf(CheckerTypeAlreadyExistsException.class);
        verify(checkers, never()).save(any());
    }

    @Test
    @DisplayName("replaces 0 com mudança não grava")
    void rejectsWhenReplacesExhausted() {
        final var existing = CheckerUseCaseFixture.persistedChecker().withReplaces(0);
        when(checkers.findById(existing.id())).thenReturn(Optional.of(existing));

        final var thrown = catchThrowable(
                () -> useCase.execute(new UpdateCheckerCommand(existing.id(), existing.type(), "novo")));

        assertThat(thrown).isInstanceOf(CheckerReplacesExhaustedException.class);
        assertThat(((CheckerReplacesExhaustedException) thrown).content().get("replaces"))
                .containsExactly(CheckerReplacesExhaustedException.MESSAGE_KEY);
        verify(checkers, never()).save(any());
    }

    @Test
    @DisplayName("Lança CheckerNotFoundException quando o id não existe")
    void throwsWhenMissing() {
        when(checkers.findById(CheckerUseCaseFixture.ID)).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(CheckerUseCaseFixture.identicalUpdateCommand()));

        assertThat(thrown).isInstanceOf(CheckerNotFoundException.class);
        verify(checkers, never()).save(any());
    }

    @Test
    @DisplayName("type nulo: não consulta o repositório")
    void rejectsNullType() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new UpdateCheckerCommand(CheckerUseCaseFixture.ID, null, null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotNull.class);
        verify(checkers, never()).findById(any());
    }

    @Test
    @DisplayName("Id nulo: não consulta o repositório")
    void rejectsNullId() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new UpdateCheckerCommand(null, Checker.Type.CHANGE_EMAIL, null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotNull.class);
        verify(checkers, never()).findById(any(UUID.class));
    }

}
