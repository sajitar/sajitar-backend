package com.sajitar.backend.application.usecase.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sajitar.backend.application.query.token.ListSessionsQuery;
import com.sajitar.backend.domain.port.token.SessionStore;

import jakarta.validation.ConstraintViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListSessionsUseCase")
class ListSessionsUseCaseTest {

    @Mock
    private SessionStore sessions;

    private ListSessionsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListSessionsUseCase(sessions, TokenUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Devolve as sessões ativas do perfil na ordem do store")
    void listsActiveSessions() {
        final var older = UUID.fromString("018f3c2a-7b00-7c3d-9e1a-000000000010");
        final var newer = UUID.fromString("018f3c2a-7b00-7c3d-9e1a-000000000020");
        when(sessions.activeSessionIds(TokenUseCaseFixture.PROFILE_ID)).thenReturn(List.of(older, newer));

        final var ids = useCase.execute(new ListSessionsQuery(TokenUseCaseFixture.PROFILE_ID));

        assertThat(ids).containsExactly(older, newer);
    }

    @Test
    @DisplayName("Perfil sem sessão ativa devolve lista vazia, não erro")
    void listsNothingWhenStoreIsEmpty() {
        when(sessions.activeSessionIds(TokenUseCaseFixture.PROFILE_ID)).thenReturn(List.of());

        assertThat(useCase.execute(new ListSessionsQuery(TokenUseCaseFixture.PROFILE_ID))).isEmpty();
    }

    @Test
    @DisplayName("Perfil nulo barra antes de consultar o store")
    void validatesProfileBeforeStore() {
        final var thrown = catchThrowable(() -> useCase.execute(new ListSessionsQuery(null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getPropertyPath()).hasToString("profileId");
        verify(sessions, never()).activeSessionIds(any());
    }

}
