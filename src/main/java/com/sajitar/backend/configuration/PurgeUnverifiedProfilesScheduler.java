package com.sajitar.backend.configuration;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sajitar.backend.application.usecase.profile.PurgeExpiredChangeEmailCheckersUseCase;
import com.sajitar.backend.application.usecase.profile.PurgeExpiredChangePasswordCheckersUseCase;
import com.sajitar.backend.application.usecase.profile.PurgeUnverifiedProfilesUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
class PurgeUnverifiedProfilesScheduler {

    private final PurgeUnverifiedProfilesUseCase purgeUnverifiedProfiles;

    private final PurgeExpiredChangePasswordCheckersUseCase purgeExpiredChangePasswordCheckers;

    private final PurgeExpiredChangeEmailCheckersUseCase purgeExpiredChangeEmailCheckers;

    @Scheduled(cron = "0 0 0 * * *", zone = "${sajitar.profile.unverified-purge-zone}")
    void execute() {
        try {
            purgeUnverifiedProfiles.execute();
            purgeExpiredChangePasswordCheckers.execute();
            purgeExpiredChangeEmailCheckers.execute();
        } catch (final RuntimeException exception) {
            log.error("Unverified profile purge failed", exception);
            throw exception;
        }
    }

}
