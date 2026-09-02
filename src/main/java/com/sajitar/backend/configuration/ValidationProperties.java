package com.sajitar.backend.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sajitar.domain.validation")
public record ValidationProperties(ProfileValidation profile, LimitValidation limit) {

    public record ProfileValidation(BirthdayValidation birthday) {
    }

    public record BirthdayValidation(int minAgeYears) {
    }

    public record LimitValidation(int max) {
    }

}
