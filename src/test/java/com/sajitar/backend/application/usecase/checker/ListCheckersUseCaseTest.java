package com.sajitar.backend.application.usecase.checker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

import com.sajitar.backend.application.query.checker.ListCheckersQuery;
import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.port.checker.CheckerPageCriteria;
import com.sajitar.backend.domain.port.checker.CheckerRepository;
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
    @DisplayName("Primeira página: conta apenas following e preceding permanece 0")
    void firstPageCountsOnlyFollowing() {
        final var first = CheckerUseCaseFixture.persistedChecker();
        final var last = CheckerUseCaseFixture.persistedVerifyEmail();
        when(checkers.findPage(any(CheckerPageCriteria.class))).thenReturn(List.of(first, last));
        when(checkers.countAfterCursor(any(CheckerPageCriteria.class))).thenReturn(12L);

        final var page = useCase.execute(new ListCheckersQuery(CheckerUseCaseFixture.PROFILE_ID, 10, false, null));

        assertThat(page.content()).containsExactly(first, last);
        assertThat(page.precedingElements()).isZero();
        assertThat(page.followingElements()).isEqualTo(12L);
        assertThat(page.reverse()).isFalse();
        verify(checkers).findPage(new CheckerPageCriteria(CheckerUseCaseFixture.PROFILE_ID, null, 10, false));
        verify(checkers).countAfterCursor(
                new CheckerPageCriteria(CheckerUseCaseFixture.PROFILE_ID, last.type(), 10, false));
    }

    @Test
    @DisplayName("Continuação de cursor: calcula preceding e following")
    void continuationCountsPrecedingAndFollowing() {
        final var first = CheckerUseCaseFixture.persistedVerifyEmail();
        final var last = CheckerUseCaseFixture.persistedChecker();
        when(checkers.findPage(any(CheckerPageCriteria.class))).thenReturn(List.of(first, last));
        when(checkers.countAfterCursor(any(CheckerPageCriteria.class))).thenReturn(4L, 7L);

        final var page = useCase.execute(new ListCheckersQuery(
                CheckerUseCaseFixture.PROFILE_ID, 2, false, Checker.Type.CHANGE_EMAIL));

        assertThat(page.followingElements()).isEqualTo(4L);
        assertThat(page.precedingElements()).isEqualTo(7L);
        assertThat(page.reverse()).isFalse();
        verify(checkers).findPage(new CheckerPageCriteria(
                CheckerUseCaseFixture.PROFILE_ID, Checker.Type.CHANGE_EMAIL, 2, false));
        verify(checkers, times(2)).countAfterCursor(any(CheckerPageCriteria.class));
    }

    @Test
    @DisplayName("Página vazia reverse ecoa o critério e não conta cursores")
    void emptyPageDoesNotCount() {
        when(checkers.findPage(any(CheckerPageCriteria.class))).thenReturn(List.of());

        final var page = useCase.execute(new ListCheckersQuery(
                CheckerUseCaseFixture.PROFILE_ID, 2, true, Checker.Type.CHANGE_EMAIL));

        assertThat(page.isEmpty()).isTrue();
        assertThat(page.reverse()).isTrue();
        verify(checkers).findPage(new CheckerPageCriteria(
                CheckerUseCaseFixture.PROFILE_ID, Checker.Type.CHANGE_EMAIL, 2, true));
        verify(checkers, never()).countAfterCursor(any());
    }

    @Test
    @DisplayName("Primeira página reverse: following na direção descendente")
    void firstPageReverseCountsFollowingDescending() {
        final var first = CheckerUseCaseFixture.persistedVerifyEmail();
        final var last = CheckerUseCaseFixture.persistedChecker();
        when(checkers.findPage(any(CheckerPageCriteria.class))).thenReturn(List.of(first, last));
        when(checkers.countAfterCursor(any(CheckerPageCriteria.class))).thenReturn(3L);

        final var page = useCase.execute(new ListCheckersQuery(CheckerUseCaseFixture.PROFILE_ID, 10, true, null));

        assertThat(page.reverse()).isTrue();
        assertThat(page.precedingElements()).isZero();
        assertThat(page.followingElements()).isEqualTo(3L);
        verify(checkers).findPage(new CheckerPageCriteria(CheckerUseCaseFixture.PROFILE_ID, null, 10, true));
        verify(checkers).countAfterCursor(
                new CheckerPageCriteria(CheckerUseCaseFixture.PROFILE_ID, last.type(), 10, true));
    }

    @Test
    @DisplayName("Continuação reverse: preceding na direção oposta")
    void continuationReverseCountsPrecedingOpposite() {
        final var first = CheckerUseCaseFixture.persistedVerifyEmail();
        final var last = CheckerUseCaseFixture.persistedChecker();
        when(checkers.findPage(any(CheckerPageCriteria.class))).thenReturn(List.of(first, last));
        when(checkers.countAfterCursor(any(CheckerPageCriteria.class))).thenReturn(0L, 1L);

        final var page = useCase.execute(new ListCheckersQuery(
                CheckerUseCaseFixture.PROFILE_ID, 2, true, Checker.Type.CHANGE_PASSWORD));

        assertThat(page.followingElements()).isZero();
        assertThat(page.precedingElements()).isEqualTo(1L);
        assertThat(page.reverse()).isTrue();
        verify(checkers).findPage(new CheckerPageCriteria(
                CheckerUseCaseFixture.PROFILE_ID, Checker.Type.CHANGE_PASSWORD, 2, true));
        verify(checkers).countAfterCursor(
                new CheckerPageCriteria(CheckerUseCaseFixture.PROFILE_ID, last.type(), 2, true));
        verify(checkers).countAfterCursor(
                new CheckerPageCriteria(CheckerUseCaseFixture.PROFILE_ID, first.type(), 2, false));
    }

    @Test
    @DisplayName("profileId nulo: não consulta o repositório")
    void rejectsNullProfileId() {
        final var thrown = catchThrowable(() -> useCase.execute(new ListCheckersQuery(null, 10, false, null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotNull.class);
        verify(checkers, never()).findPage(any());
        verify(checkers, never()).countAfterCursor(any());
    }

    @Test
    @DisplayName("reverse nulo: não consulta o repositório")
    void rejectsNullReverse() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new ListCheckersQuery(CheckerUseCaseFixture.PROFILE_ID, 10, null, null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotNull.class);
        verify(checkers, never()).findPage(any());
    }

    @Test
    @DisplayName("limit inválido: não consulta o repositório")
    void rejectsInvalidLimit() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new ListCheckersQuery(CheckerUseCaseFixture.PROFILE_ID, 0, false, null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(checkers, never()).findPage(any());
    }

    @Test
    @DisplayName("limit nulo: não consulta o repositório")
    void rejectsNullLimit() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new ListCheckersQuery(CheckerUseCaseFixture.PROFILE_ID, null, false, null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(checkers, never()).findPage(any());
    }

    @Test
    @DisplayName("limit acima do máximo: não consulta o repositório")
    void rejectsLimitAboveMax() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new ListCheckersQuery(CheckerUseCaseFixture.PROFILE_ID, 101, false, null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(checkers, never()).findPage(any());
    }

}
