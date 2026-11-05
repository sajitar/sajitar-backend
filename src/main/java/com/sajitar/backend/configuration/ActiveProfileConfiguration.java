package com.sajitar.backend.configuration;

import java.util.Set;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
class ActiveProfileConfiguration {

    private static final Set<String> ALLOWED = Set.of(
            "LOCAL", "LOCAL-TEST", "CI", "PROD", "DOCKER", "DOCKER-TEST", "DEMO");

    ActiveProfileConfiguration(final Environment environment) {
        final var active = environment.getActiveProfiles();
        if (active.length != 1 || !ALLOWED.contains(active[0])) {
            throw new IllegalStateException("SPRING_PROFILES_ACTIVE must be set");
        }
    }

}
