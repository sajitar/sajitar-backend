package com.sajitar.backend.configuration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sajitar.backend.application.usecase.profile.PurgeExpiredChangePasswordCheckersUseCase;
import com.sajitar.backend.application.usecase.profile.PurgeUnverifiedProfilesUseCase;

@DisplayName("PurgeUnverifiedProfilesScheduler")
class PurgeUnverifiedProfilesSchedulerTest {

    @Test
    @DisplayName("Delega a varredura aos use cases")
    void delegatesToUseCases() {
        final var unverified = mock(PurgeUnverifiedProfilesUseCase.class);
        final var changePassword = mock(PurgeExpiredChangePasswordCheckersUseCase.class);
        final var scheduler = new PurgeUnverifiedProfilesScheduler(unverified, changePassword);

        scheduler.execute();

        verify(unverified).execute();
        verify(changePassword).execute();
    }

}
