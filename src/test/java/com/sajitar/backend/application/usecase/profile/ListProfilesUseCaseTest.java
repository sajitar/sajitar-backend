package com.sajitar.backend.application.usecase.profile;

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

import com.sajitar.backend.application.query.profile.ListProfilesQuery;
import com.sajitar.backend.application.query.profile.ProfileCursor;
import com.sajitar.backend.domain.port.profile.ProfilePageCriteria;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.validation.Limit;
import com.sajitar.backend.domain.validation.profile.Birthday;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListProfilesUseCase")
class ListProfilesUseCaseTest {

    @Mock
    private ProfileRepository profiles;

    private ListProfilesUseCase useCase;

    @BeforeAll
    static void configureValidation() {
        Birthday.BirthdayValidator.configure(18);
        Limit.LimitValidator.configure(100);
    }

    @BeforeEach
    void setUp() {
        useCase = new ListProfilesUseCase(profiles, ProfileUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Primeira página: conta apenas following e preceding permanece 0")
    void firstPageCountsOnlyFollowing() {
        final var first = ProfileUseCaseFixture.persistedProfile();
        final var last = first.withId(java.util.UUID.fromString("550e8400-e29b-41d4-a716-446655440001")).withName("Zelia");
        when(profiles.findPage(any(ProfilePageCriteria.class))).thenReturn(List.of(first, last));
        when(profiles.countAfterCursor(any(ProfilePageCriteria.class))).thenReturn(12L);

        final var page = useCase.execute(new ListProfilesQuery(10, false, null, null));

        assertThat(page.content()).containsExactly(first, last);
        assertThat(page.precedingElements()).isZero();
        assertThat(page.followingElements()).isEqualTo(12L);
        assertThat(page.reverse()).isFalse();
        verify(profiles).findPage(any(ProfilePageCriteria.class));
        verify(profiles).countAfterCursor(any(ProfilePageCriteria.class));
    }

    @Test
    @DisplayName("Continuação de cursor: calcula preceding e following")
    void continuationCountsPrecedingAndFollowing() {
        final var first = ProfileUseCaseFixture.persistedProfile();
        final var last = first.withId(java.util.UUID.fromString("550e8400-e29b-41d4-a716-446655440001")).withName("Zelia");
        when(profiles.findPage(any(ProfilePageCriteria.class))).thenReturn(List.of(first, last));
        when(profiles.countAfterCursor(any(ProfilePageCriteria.class))).thenReturn(4L, 7L);

        final var page = useCase.execute(new ListProfilesQuery(
                10,
                false,
                null,
                new ProfileCursor(first.name(), first.id())));

        assertThat(page.followingElements()).isEqualTo(4L);
        assertThat(page.precedingElements()).isEqualTo(7L);
        verify(profiles).findPage(any(ProfilePageCriteria.class));
        verify(profiles, org.mockito.Mockito.times(2)).countAfterCursor(any(ProfilePageCriteria.class));
    }

    @Test
    @DisplayName("name em branco não é filtro de busca")
    void blankNameIsNotAFilter() {
        when(profiles.findPage(any(ProfilePageCriteria.class))).thenReturn(List.of());

        final var page = useCase.execute(new ListProfilesQuery(10, false, "   ", null));

        assertThat(page.isEmpty()).isTrue();
        verify(profiles).findPage(any(ProfilePageCriteria.class));
    }

    @Test
    @DisplayName("Página vazia: não conta cursores")
    void emptyPageDoesNotCount() {
        when(profiles.findPage(any(ProfilePageCriteria.class))).thenReturn(List.of());

        final var page = useCase.execute(new ListProfilesQuery(10, true, "Silva", null));

        assertThat(page.isEmpty()).isTrue();
        assertThat(page.reverse()).isTrue();
        verify(profiles).findPage(any(ProfilePageCriteria.class));
        verify(profiles, never()).countAfterCursor(any());
    }

    @Test
    @DisplayName("limit inválido: não chama o repositório")
    void doesNotCallRepositoryWhenLimitIsInvalid() {
        final var thrown = catchThrowable(() -> useCase.execute(new ListProfilesQuery(0, false, null, null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findPage(any());
    }

    @Test
    @DisplayName("reverse nulo: não chama o repositório")
    void doesNotCallRepositoryWhenReverseIsNull() {
        final var thrown = catchThrowable(() -> useCase.execute(new ListProfilesQuery(10, null, null, null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotNull.class);
        verify(profiles, never()).findPage(any());
    }

    @Test
    @DisplayName("lastSeenName inválido no cursor: não chama o repositório")
    void doesNotCallRepositoryWhenCursorNameIsInvalid() {
        final var thrown = catchThrowable(() -> useCase.execute(new ListProfilesQuery(
                10,
                false,
                null,
                new ProfileCursor("Maria@Silva", ProfileUseCaseFixture.ID))));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(Pattern.class);
        assertThat(violation.getPropertyPath().toString()).contains("lastSeenName");
        verify(profiles, never()).findPage(any());
    }

    @Test
    @DisplayName("limit acima do máximo: não chama o repositório")
    void doesNotCallRepositoryWhenLimitExceedsMax() {
        final var thrown = catchThrowable(() -> useCase.execute(new ListProfilesQuery(101, false, null, null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findPage(any());
    }

}
