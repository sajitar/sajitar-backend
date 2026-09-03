package com.sajitar.backend.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sajitar.backend.application.query.ListCheckersQuery;
import com.sajitar.backend.domain.model.Checker;
import com.sajitar.backend.domain.port.CheckerPageCriteria;
import com.sajitar.backend.domain.port.CheckerRepository;
import com.sajitar.backend.domain.validation.Limit;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListCheckersUseCase")
class ListCheckersUseCaseTest {

    @Mock
    private CheckerRepository checkers;

    private ListCheckersUseCase useCase;

    @BeforeAll
    static void configureValidation() {
        Limit.LimitValidator.configure(100);
    }

    @BeforeEach
    void setUp() {
        useCase = new ListCheckersUseCase(checkers, CheckerUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Devolve a página do repositório")
    void returnsPageFromRepository() {
        final var existing = CheckerUseCaseFixture.persistedChecker();
        when(checkers.findPage(any(CheckerPageCriteria.class))).thenReturn(List.of(existing));

        final var content = useCase.execute(new ListCheckersQuery(CheckerUseCaseFixture.PROFILE_ID, 10, null));

        assertThat(content).containsExactly(existing);
        verify(checkers).findPage(new CheckerPageCriteria(CheckerUseCaseFixture.PROFILE_ID, null, 10));
    }

    @Test
    @DisplayName("Encaminha lastSeenType no critério")
    void forwardsCursor() {
        when(checkers.findPage(any(CheckerPageCriteria.class))).thenReturn(List.of());

        final var content = useCase.execute(new ListCheckersQuery(
                CheckerUseCaseFixture.PROFILE_ID, 2, Checker.Type.CHANGE_EMAIL));

        assertThat(content).isEmpty();
        verify(checkers).findPage(new CheckerPageCriteria(
                CheckerUseCaseFixture.PROFILE_ID, Checker.Type.CHANGE_EMAIL, 2));
    }

    @Test
    @DisplayName("profileId nulo: não consulta o repositório")
    void rejectsNullProfileId() {
        final var thrown = catchThrowable(() -> useCase.execute(new ListCheckersQuery(null, 10, null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotNull.class);
        verify(checkers, never()).findPage(any());
    }

    @Test
    @DisplayName("limit inválido: não consulta o repositório")
    void rejectsInvalidLimit() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new ListCheckersQuery(CheckerUseCaseFixture.PROFILE_ID, 0, null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(checkers, never()).findPage(any());
    }

    @Test
    @DisplayName("limit nulo: não consulta o repositório")
    void rejectsNullLimit() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new ListCheckersQuery(CheckerUseCaseFixture.PROFILE_ID, null, null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(checkers, never()).findPage(any());
    }

    @Test
    @DisplayName("limit acima do máximo: não consulta o repositório")
    void rejectsLimitAboveMax() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new ListCheckersQuery(CheckerUseCaseFixture.PROFILE_ID, 101, null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(checkers, never()).findPage(any());
    }

}
