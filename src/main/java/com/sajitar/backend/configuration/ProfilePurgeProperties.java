package com.sajitar.backend.configuration;

import java.time.DateTimeException;
import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sajitar.profile")
public record ProfilePurgeProperties(
        int unverifiedMaxAgeMinutes,
        int changePasswordMaxAgeMinutes,
        int changeEmailMaxAgeMinutes,
        int deleteProfileMaxAgeMinutes,
        int signInMaxAgeMinutes,
        String unverifiedPurgeZone) {

    public ProfilePurgeProperties {
        if (unverifiedMaxAgeMinutes <= 0) {
            throw new IllegalArgumentException(
                    "sajitar.profile.unverified-max-age-minutes must be greater than 0");
        }
        if (changePasswordMaxAgeMinutes <= 0) {
            throw new IllegalArgumentException(
                    "sajitar.profile.change-password-max-age-minutes must be greater than 0");
        }
        if (changeEmailMaxAgeMinutes <= 0) {
            throw new IllegalArgumentException(
                    "sajitar.profile.change-email-max-age-minutes must be greater than 0");
        }
        if (deleteProfileMaxAgeMinutes <= 0) {
            throw new IllegalArgumentException(
                    "sajitar.profile.delete-profile-max-age-minutes must be greater than 0");
        }
        if (signInMaxAgeMinutes <= 0) {
            throw new IllegalArgumentException(
                    "sajitar.profile.sign-in-max-age-minutes must be greater than 0");
        }
        if (unverifiedPurgeZone == null || unverifiedPurgeZone.isBlank()) {
            throw new IllegalArgumentException("sajitar.profile.unverified-purge-zone must not be blank");
        }
        try {
            ZoneId.of(unverifiedPurgeZone);
        } catch (final DateTimeException _) {
            throw new IllegalArgumentException("sajitar.profile.unverified-purge-zone must be a valid time-zone");
        }
    }

}
