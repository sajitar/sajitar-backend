package com.sajitar.backend.application.usecase.profile;

import java.time.Clock;
import java.time.Duration;

import org.springframework.stereotype.Service;

import com.sajitar.backend.configuration.ProfilePurgeProperties;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.port.token.SessionStore;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PurgeUnverifiedProfilesUseCase {

    private final ProfileRepository profiles;

    private final SessionStore sessions;

    private final ProfilePurgeProperties properties;

    private final Clock clock;

    public void execute() {
        final var cutoff = clock.instant().minus(Duration.ofHours(properties.unverifiedMaxAgeHours()));
        for (final var id : profiles.findUnverifiedCreatedBefore(cutoff)) {
            sessions.wipe(id);
            profiles.deleteById(id);
        }
    }

}
