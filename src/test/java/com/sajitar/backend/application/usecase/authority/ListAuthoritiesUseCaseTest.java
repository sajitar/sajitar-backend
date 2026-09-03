package com.sajitar.backend.application.usecase.authority;

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

import com.sajitar.backend.application.query.authority.ListAuthoritiesQuery;
import com.sajitar.backend.domain.model.authority.Authority;
import com.sajitar.backend.domain.port.authority.AuthorityPageCriteria;
import com.sajitar.backend.domain.port.authority.AuthorityRepository;
import com.sajitar.backend.domain.validation.Limit;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListAuthoritiesUseCase")
class ListAuthoritiesUseCaseTest {

    @Mock
    private AuthorityRepository authorities;

    private ListAuthoritiesUseCase useCase;

    @BeforeAll
    static void configureValidation() {
        Limit.LimitValidator.configure(100);
    }

    @BeforeEach
    void setUp() {
        useCase = new ListAuthoritiesUseCase(authorities, AuthorityUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Primeira página: conta apenas following e preceding permanece 0")
    void firstPageCountsOnlyFollowing() {
        final var first = AuthorityUseCaseFixture.persistedMaster();
        final var last = AuthorityUseCaseFixture.persistedMember();
        when(authorities.findPage(any(AuthorityPageCriteria.class))).thenReturn(List.of(first, last));
        when(authorities.countAfterCursor(any(AuthorityPageCriteria.class))).thenReturn(12L);

        final var page = useCase.execute(new ListAuthoritiesQuery(AuthorityUseCaseFixture.PROFILE_ID, 10, false, null));

        assertThat(page.content()).containsExactly(first, last);
        assertThat(page.precedingElements()).isZero();
        assertThat(page.followingElements()).isEqualTo(12L);
        assertThat(page.reverse()).isFalse();
        verify(authorities).findPage(new AuthorityPageCriteria(AuthorityUseCaseFixture.PROFILE_ID, null, 10, false));
        verify(authorities).countAfterCursor(
                new AuthorityPageCriteria(AuthorityUseCaseFixture.PROFILE_ID, last.type(), 10, false));
    }

    @Test
    @DisplayName("Continuação de cursor: calcula preceding e following")
    void continuationCountsPrecedingAndFollowing() {
        final var first = AuthorityUseCaseFixture.persistedMember();
        final var last = AuthorityUseCaseFixture.persistedMaster();
        when(authorities.findPage(any(AuthorityPageCriteria.class))).thenReturn(List.of(first, last));
        when(authorities.countAfterCursor(any(AuthorityPageCriteria.class))).thenReturn(4L, 7L);

        final var page = useCase.execute(new ListAuthoritiesQuery(
                AuthorityUseCaseFixture.PROFILE_ID, 2, false, Authority.Type.MASTER));

        assertThat(page.followingElements()).isEqualTo(4L);
        assertThat(page.precedingElements()).isEqualTo(7L);
        assertThat(page.reverse()).isFalse();
        verify(authorities).findPage(new AuthorityPageCriteria(
                AuthorityUseCaseFixture.PROFILE_ID, Authority.Type.MASTER, 2, false));
        verify(authorities, times(2)).countAfterCursor(any(AuthorityPageCriteria.class));
    }

    @Test
    @DisplayName("Página vazia reverse ecoa o critério e não conta cursores")
    void emptyPageDoesNotCount() {
        when(authorities.findPage(any(AuthorityPageCriteria.class))).thenReturn(List.of());

        final var page = useCase.execute(new ListAuthoritiesQuery(
                AuthorityUseCaseFixture.PROFILE_ID, 2, true, Authority.Type.MASTER));

        assertThat(page.isEmpty()).isTrue();
        assertThat(page.reverse()).isTrue();
        verify(authorities).findPage(new AuthorityPageCriteria(
                AuthorityUseCaseFixture.PROFILE_ID, Authority.Type.MASTER, 2, true));
        verify(authorities, never()).countAfterCursor(any());
    }

    @Test
    @DisplayName("Primeira página reverse: following na direção descendente")
    void firstPageReverseCountsFollowingDescending() {
        final var first = AuthorityUseCaseFixture.persistedMember();
        final var last = AuthorityUseCaseFixture.persistedMaster();
        when(authorities.findPage(any(AuthorityPageCriteria.class))).thenReturn(List.of(first, last));
        when(authorities.countAfterCursor(any(AuthorityPageCriteria.class))).thenReturn(3L);

        final var page = useCase.execute(new ListAuthoritiesQuery(AuthorityUseCaseFixture.PROFILE_ID, 10, true, null));

        assertThat(page.reverse()).isTrue();
        assertThat(page.precedingElements()).isZero();
        assertThat(page.followingElements()).isEqualTo(3L);
        verify(authorities).findPage(new AuthorityPageCriteria(AuthorityUseCaseFixture.PROFILE_ID, null, 10, true));
        verify(authorities).countAfterCursor(
                new AuthorityPageCriteria(AuthorityUseCaseFixture.PROFILE_ID, last.type(), 10, true));
    }

    @Test
    @DisplayName("Continuação reverse: preceding na direção oposta")
    void continuationReverseCountsPrecedingOpposite() {
        final var first = AuthorityUseCaseFixture.persistedMember();
        final var last = AuthorityUseCaseFixture.persistedMaster();
        when(authorities.findPage(any(AuthorityPageCriteria.class))).thenReturn(List.of(first, last));
        when(authorities.countAfterCursor(any(AuthorityPageCriteria.class))).thenReturn(0L, 1L);

        final var page = useCase.execute(new ListAuthoritiesQuery(
                AuthorityUseCaseFixture.PROFILE_ID, 2, true, Authority.Type.READER));

        assertThat(page.followingElements()).isZero();
        assertThat(page.precedingElements()).isEqualTo(1L);
        assertThat(page.reverse()).isTrue();
        verify(authorities).findPage(new AuthorityPageCriteria(
                AuthorityUseCaseFixture.PROFILE_ID, Authority.Type.READER, 2, true));
        verify(authorities).countAfterCursor(
                new AuthorityPageCriteria(AuthorityUseCaseFixture.PROFILE_ID, last.type(), 2, true));
        verify(authorities).countAfterCursor(
                new AuthorityPageCriteria(AuthorityUseCaseFixture.PROFILE_ID, first.type(), 2, false));
    }

    @Test
    @DisplayName("profileId nulo: não consulta o repositório")
    void rejectsNullProfileId() {
        final var thrown = catchThrowable(() -> useCase.execute(new ListAuthoritiesQuery(null, 10, false, null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotNull.class);
        verify(authorities, never()).findPage(any());
        verify(authorities, never()).countAfterCursor(any());
    }

    @Test
    @DisplayName("reverse nulo: não consulta o repositório")
    void rejectsNullReverse() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new ListAuthoritiesQuery(AuthorityUseCaseFixture.PROFILE_ID, 10, null, null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotNull.class);
        verify(authorities, never()).findPage(any());
    }

    @Test
    @DisplayName("limit inválido: não consulta o repositório")
    void rejectsInvalidLimit() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new ListAuthoritiesQuery(AuthorityUseCaseFixture.PROFILE_ID, 0, false, null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(authorities, never()).findPage(any());
    }

    @Test
    @DisplayName("limit nulo: não consulta o repositório")
    void rejectsNullLimit() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new ListAuthoritiesQuery(AuthorityUseCaseFixture.PROFILE_ID, null, false, null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(authorities, never()).findPage(any());
    }

    @Test
    @DisplayName("limit acima do máximo: não consulta o repositório")
    void rejectsLimitAboveMax() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new ListAuthoritiesQuery(AuthorityUseCaseFixture.PROFILE_ID, 101, false, null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(authorities, never()).findPage(any());
    }

}
