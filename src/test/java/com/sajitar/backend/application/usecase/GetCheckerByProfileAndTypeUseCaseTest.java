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

import com.sajitar.backend.application.query.GetCheckerByProfileAndTypeQuery;
import com.sajitar.backend.domain.model.Checker;
import com.sajitar.backend.domain.port.CheckerRepository;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetCheckerByProfileAndTypeUseCase")
class GetCheckerByProfileAndTypeUseCaseTest {

    @Mock
    private CheckerRepository checkers;

    private GetCheckerByProfileAndTypeUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetCheckerByProfileAndTypeUseCase(checkers, CheckerUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Retorna o checker do par quando existe")
    void returnsWhenFound() {
        final var existing = CheckerUseCaseFixture.persistedChecker();
        when(checkers.findByProfileIdAndType(existing.profileId(), existing.type())).thenReturn(Optional.of(existing));

        final var result = useCase.execute(new GetCheckerByProfileAndTypeQuery(existing.profileId(), existing.type()));

        assertThat(result).contains(existing);
    }

    @Test
    @DisplayName("Retorna vazio quando o par não existe")
    void returnsEmptyWhenMissing() {
        when(checkers.findByProfileIdAndType(CheckerUseCaseFixture.PROFILE_ID, Checker.Type.CHANGE_PASSWORD))
                .thenReturn(Optional.empty());

        final var result = useCase.execute(new GetCheckerByProfileAndTypeQuery(
                CheckerUseCaseFixture.PROFILE_ID, Checker.Type.CHANGE_PASSWORD));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("profileId nulo: não consulta o repositório")
    void rejectsNullProfileId() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new GetCheckerByProfileAndTypeQuery(null, Checker.Type.CHANGE_EMAIL)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotNull.class);
        verify(checkers, never()).findByProfileIdAndType(any(), any());
    }

    @Test
    @DisplayName("type nulo: não consulta o repositório")
    void rejectsNullType() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new GetCheckerByProfileAndTypeQuery(CheckerUseCaseFixture.PROFILE_ID, null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(checkers, never()).findByProfileIdAndType(any(UUID.class), any());
    }

}
