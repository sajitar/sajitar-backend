package com.sajitar.backend.configuration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sajitar.backend.application.usecase.profile.PurgeExpiredChangeEmailCheckersUseCase;
import com.sajitar.backend.application.usecase.profile.PurgeExpiredChangePasswordCheckersUseCase;
import com.sajitar.backend.application.usecase.profile.PurgeExpiredDeleteProfileCheckersUseCase;
import com.sajitar.backend.application.usecase.profile.PurgeExpiredSignInCheckersUseCase;
import com.sajitar.backend.application.usecase.profile.PurgeUnverifiedProfilesUseCase;

@DisplayName("PurgeUnverifiedProfilesScheduler")
class PurgeUnverifiedProfilesSchedulerTest {

    @Test
    @DisplayName("Delega a varredura aos use cases")
    void delegatesToUseCases() {
        final var unverified = mock(PurgeUnverifiedProfilesUseCase.class);
        final var changePassword = mock(PurgeExpiredChangePasswordCheckersUseCase.class);
        final var changeEmail = mock(PurgeExpiredChangeEmailCheckersUseCase.class);
        final var deleteProfile = mock(PurgeExpiredDeleteProfileCheckersUseCase.class);
        final var signIn = mock(PurgeExpiredSignInCheckersUseCase.class);
        final var scheduler = new PurgeUnverifiedProfilesScheduler(
                unverified, changePassword, changeEmail, deleteProfile, signIn);

        scheduler.execute();

        verify(unverified).execute();
        verify(changePassword).execute();
        verify(changeEmail).execute();
        verify(deleteProfile).execute();
        verify(signIn).execute();
    }

}
