package com.sajitar.backend.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.sajitar.backend.application.usecase.profile.PurgeExpiredChangeEmailCheckersUseCase;
import com.sajitar.backend.application.usecase.profile.PurgeExpiredChangePasswordCheckersUseCase;
import com.sajitar.backend.application.usecase.profile.PurgeExpiredDeleteProfileCheckersUseCase;
import com.sajitar.backend.application.usecase.profile.PurgeExpiredSignInCheckersUseCase;
import com.sajitar.backend.application.usecase.profile.PurgeUnverifiedProfilesUseCase;

@DisplayName("PurgeUnverifiedProfilesScheduler")
class PurgeUnverifiedProfilesSchedulerTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(PurgeUnverifiedProfilesUseCase.class, () -> mock(PurgeUnverifiedProfilesUseCase.class))
            .withBean(PurgeExpiredChangePasswordCheckersUseCase.class,
                    () -> mock(PurgeExpiredChangePasswordCheckersUseCase.class))
            .withBean(PurgeExpiredChangeEmailCheckersUseCase.class,
                    () -> mock(PurgeExpiredChangeEmailCheckersUseCase.class))
            .withBean(PurgeExpiredDeleteProfileCheckersUseCase.class,
                    () -> mock(PurgeExpiredDeleteProfileCheckersUseCase.class))
            .withBean(PurgeExpiredSignInCheckersUseCase.class, () -> mock(PurgeExpiredSignInCheckersUseCase.class))
            .withUserConfiguration(PurgeUnverifiedProfilesScheduler.class);

    @ParameterizedTest
    @ValueSource(strings = { "LOCAL", "CI", "PROD", "DOCKER", "DEMO" })
    @DisplayName("Registra o agendador fora dos perfis LOCAL-TEST e DOCKER-TEST")
    void registersOutsideTest(final String profile) {
        runner.withInitializer(context -> context.getEnvironment().setActiveProfiles(profile))
                .run(context -> assertThat(context).hasSingleBean(PurgeUnverifiedProfilesScheduler.class));
    }

    @ParameterizedTest
    @ValueSource(strings = { "LOCAL-TEST", "DOCKER-TEST" })
    @DisplayName("Não registra o agendador nos perfis LOCAL-TEST e DOCKER-TEST")
    void skipsTestProfile(final String profile) {
        runner.withInitializer(context -> context.getEnvironment().setActiveProfiles(profile))
                .run(context -> assertThat(context).doesNotHaveBean(PurgeUnverifiedProfilesScheduler.class));
    }

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
