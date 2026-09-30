package com.sajitar.backend.configuration;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sajitar.backend.application.usecase.profile.PurgeExpiredChangeEmailCheckersUseCase;
import com.sajitar.backend.application.usecase.profile.PurgeExpiredChangePasswordCheckersUseCase;
import com.sajitar.backend.application.usecase.profile.PurgeExpiredDeleteProfileCheckersUseCase;
import com.sajitar.backend.application.usecase.profile.PurgeUnverifiedProfilesUseCase;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class PurgeUnverifiedProfilesScheduler {

    private final PurgeUnverifiedProfilesUseCase purgeUnverifiedProfiles;

    private final PurgeExpiredChangePasswordCheckersUseCase purgeExpiredChangePasswordCheckers;

    private final PurgeExpiredChangeEmailCheckersUseCase purgeExpiredChangeEmailCheckers;

    private final PurgeExpiredDeleteProfileCheckersUseCase purgeExpiredDeleteProfileCheckers;

    @Scheduled(cron = "0 */5 * * * *", zone = "${sajitar.profile.unverified-purge-zone}")
    void execute() {
        purgeUnverifiedProfiles.execute();
        purgeExpiredChangePasswordCheckers.execute();
        purgeExpiredChangeEmailCheckers.execute();
        purgeExpiredDeleteProfileCheckers.execute();
    }

}
