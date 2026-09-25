package com.sajitar.backend.configuration;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sajitar.backend.application.usecase.profile.PurgeExpiredChangePasswordCheckersUseCase;
import com.sajitar.backend.application.usecase.profile.PurgeUnverifiedProfilesUseCase;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class PurgeUnverifiedProfilesScheduler {

    private final PurgeUnverifiedProfilesUseCase purgeUnverifiedProfiles;

    private final PurgeExpiredChangePasswordCheckersUseCase purgeExpiredChangePasswordCheckers;

    @Scheduled(cron = "0 0 0 * * *", zone = "${sajitar.profile.unverified-purge-zone}")
    void execute() {
        purgeUnverifiedProfiles.execute();
        purgeExpiredChangePasswordCheckers.execute();
    }

}
