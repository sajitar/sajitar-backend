package com.sajitar.backend.application.usecase.profile;

import java.time.Clock;
import java.time.Duration;

import org.springframework.stereotype.Service;

import com.sajitar.backend.configuration.ProfilePurgeProperties;
import com.sajitar.backend.domain.port.checker.CheckerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PurgeExpiredChangeEmailCheckersUseCase {

    private final CheckerRepository checkers;

    private final ProfilePurgeProperties properties;

    private final Clock clock;

    public void execute() {
        final var cutoff = clock.instant().minus(Duration.ofHours(properties.changeEmailMaxAgeHours()));
        for (final var id : checkers.findChangeEmailCreatedBefore(cutoff)) {
            checkers.deleteById(id);
        }
    }

}
