package com.sajitar.backend.application.usecase.profile;

import static org.mockito.ArgumentMatchers.any;
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
import com.sajitar.backend.domain.port.checker.CheckerRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("PurgeExpiredChangePasswordCheckersUseCase")
class PurgeExpiredChangePasswordCheckersUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-09-21T03:00:00Z");

    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private static final ProfilePurgeProperties PROPERTIES = new ProfilePurgeProperties(48, 12, "UTC");

    private static final UUID FIRST = UUID.fromString("019c1000-a113-7000-8000-333333333333");

    private static final UUID SECOND = UUID.fromString("019c1000-b113-7000-8000-444444444444");

    @Mock
    private CheckerRepository checkers;

    private PurgeExpiredChangePasswordCheckersUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new PurgeExpiredChangePasswordCheckersUseCase(checkers, PROPERTIES, CLOCK);
    }

    @Test
    @DisplayName("Lista vazia não chama delete")
    void emptyListDoesNothing() {
        when(checkers.findChangePasswordCreatedBefore(NOW.minus(Duration.ofHours(12)))).thenReturn(List.of());

        useCase.execute();

        verify(checkers, never()).deleteById(any());
    }

    @Test
    @DisplayName("Apaga cada checker na ordem da lista, sem wipe")
    void deletesEachIdInOrder() {
        when(checkers.findChangePasswordCreatedBefore(NOW.minus(Duration.ofHours(12))))
                .thenReturn(List.of(FIRST, SECOND));

        useCase.execute();

        final var order = inOrder(checkers);
        order.verify(checkers).findChangePasswordCreatedBefore(NOW.minus(Duration.ofHours(12)));
        order.verify(checkers).deleteById(FIRST);
        order.verify(checkers).deleteById(SECOND);
    }

}
