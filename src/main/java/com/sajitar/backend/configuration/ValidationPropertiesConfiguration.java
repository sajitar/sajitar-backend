package com.sajitar.backend.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.sajitar.backend.domain.validation.Limit;
import com.sajitar.backend.domain.validation.profile.Birthday;

@Configuration
@EnableConfigurationProperties(ValidationProperties.class)
class ValidationPropertiesConfiguration {

    ValidationPropertiesConfiguration(final ValidationProperties properties) {
        Birthday.BirthdayValidator.configure(properties.profile().birthday().minAgeYears());
        Limit.LimitValidator.configure(properties.limit().max());
    }

}
