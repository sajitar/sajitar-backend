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

import com.sajitar.backend.domain.port.checker.CheckerRepository;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetCheckerUseCase")
class GetCheckerUseCaseTest {

    @Mock
    private CheckerRepository checkers;

    private GetCheckerUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetCheckerUseCase(checkers, CheckerUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Delega ao repositório quando o id é válido")
    void delegatesWhenIdIsValid() {
        when(checkers.findById(CheckerUseCaseFixture.ID)).thenReturn(Optional.empty());

        assertThat(useCase.execute(CheckerUseCaseFixture.ID)).isEmpty();
        verify(checkers).findById(CheckerUseCaseFixture.ID);
    }

    @Test
    @DisplayName("Retorna o checker quando encontrado")
    void returnsWhenFound() {
        final var existing = CheckerUseCaseFixture.persistedChecker();
        when(checkers.findById(existing.id())).thenReturn(Optional.of(existing));

        assertThat(useCase.execute(existing.id())).contains(existing);
    }

    @Test
    @DisplayName("Id nulo: não chama o repositório")
    void rejectsNullId() {
        final var thrown = catchThrowable(() -> useCase.execute(null));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotNull.class);
        verify(checkers, never()).findById(any(UUID.class));
    }

}
