package com.sajitar.backend.configuration;

import java.time.DateTimeException;
import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sajitar.profile")
public record ProfilePurgeProperties(
        int unverifiedMaxAgeHours,
        int changePasswordMaxAgeHours,
        String unverifiedPurgeZone) {

    public ProfilePurgeProperties {
        if (unverifiedMaxAgeHours <= 0) {
            throw new IllegalArgumentException(
                    "sajitar.profile.unverified-max-age-hours must be greater than 0");
        }
        if (changePasswordMaxAgeHours <= 0) {
            throw new IllegalArgumentException(
                    "sajitar.profile.change-password-max-age-hours must be greater than 0");
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
