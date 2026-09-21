package com.sajitar.backend.application.usecase.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sajitar.backend.configuration.ProfilePurgeProperties;
import com.sajitar.backend.domain.exception.SessionStoreUnavailableException;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.port.token.SessionStore;

@ExtendWith(MockitoExtension.class)
@DisplayName("PurgeUnverifiedProfilesUseCase")
class PurgeUnverifiedProfilesUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-09-21T03:00:00Z");

    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private static final ProfilePurgeProperties PROPERTIES = new ProfilePurgeProperties(48, "UTC");

    private static final UUID FIRST = UUID.fromString("01989bad-6161-7000-0ae9-f440b10578ec");

    private static final UUID SECOND = UUID.fromString("01989bad-7000-7000-0ae9-f440b10578ec");

    @Mock
    private ProfileRepository profiles;

    @Mock
    private SessionStore sessions;

    private PurgeUnverifiedProfilesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new PurgeUnverifiedProfilesUseCase(profiles, sessions, PROPERTIES, CLOCK);
    }

    @Test
    @DisplayName("Lista vazia não chama wipe nem delete")
    void emptyListDoesNothing() {
        when(profiles.findUnverifiedCreatedBefore(NOW.minus(Duration.ofHours(48)))).thenReturn(List.of());

        useCase.execute();

        verify(sessions, never()).wipe(any());
        verify(profiles, never()).deleteById(any());
    }

    @Test
    @DisplayName("Wipe precede o delete em cada id, na ordem da lista")
    void wipesThenDeletesEachIdInOrder() {
        when(profiles.findUnverifiedCreatedBefore(NOW.minus(Duration.ofHours(48))))
                .thenReturn(List.of(FIRST, SECOND));

        useCase.execute();

        final var order = inOrder(sessions, profiles);
        order.verify(profiles).findUnverifiedCreatedBefore(NOW.minus(Duration.ofHours(48)));
        order.verify(sessions).wipe(FIRST);
        order.verify(profiles).deleteById(FIRST);
        order.verify(sessions).wipe(SECOND);
        order.verify(profiles).deleteById(SECOND);
    }

    @Test
    @DisplayName("Store fora do ar não exclui o id corrente nem segue o lote")
    void keepsCurrentProfileWhenSessionStoreIsDown() {
        when(profiles.findUnverifiedCreatedBefore(NOW.minus(Duration.ofHours(48))))
                .thenReturn(List.of(FIRST, SECOND));
        doThrow(new SessionStoreUnavailableException()).when(sessions).wipe(FIRST);

        final var thrown = catchThrowable(() -> useCase.execute());

        assertThat(thrown).isInstanceOf(SessionStoreUnavailableException.class);
        verify(profiles, never()).deleteById(any());
        verify(sessions, never()).wipe(SECOND);
    }

}
