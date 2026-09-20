package com.sajitar.backend.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
class ActiveProfileConfiguration {

    ActiveProfileConfiguration(final Environment environment) {
        if (environment.getActiveProfiles().length == 0) {
            throw new IllegalStateException("SPRING_PROFILES_ACTIVE must be set");
        }
    }

}
