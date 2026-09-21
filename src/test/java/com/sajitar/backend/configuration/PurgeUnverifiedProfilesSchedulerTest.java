package com.sajitar.backend.configuration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sajitar.backend.application.usecase.profile.PurgeUnverifiedProfilesUseCase;

@DisplayName("PurgeUnverifiedProfilesScheduler")
class PurgeUnverifiedProfilesSchedulerTest {

    @Test
    @DisplayName("Delega a varredura ao use case")
    void delegatesToUseCase() {
        final var useCase = mock(PurgeUnverifiedProfilesUseCase.class);
        final var scheduler = new PurgeUnverifiedProfilesScheduler(useCase);

        scheduler.execute();

        verify(useCase).execute();
    }

}
