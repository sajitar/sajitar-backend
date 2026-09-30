package com.sajitar.backend.application.usecase.profile;

import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.command.profile.DeleteProfileCommand;
import com.sajitar.backend.configuration.ProfilePurgeProperties;
import com.sajitar.backend.domain.exception.EmailNotVerifiedException;
import com.sajitar.backend.domain.exception.ForbiddenProfileDeletionException;
import com.sajitar.backend.domain.exception.InvalidCheckerVerificationException;
import com.sajitar.backend.domain.exception.ProfileNotFoundException;
import com.sajitar.backend.domain.exception.TooManyAttemptsException;
import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.model.token.AttemptScope;
import com.sajitar.backend.domain.port.checker.CheckerRepository;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.port.token.AttemptLimiter;
import com.sajitar.backend.domain.port.token.SessionStore;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeleteProfileUseCase {

    private final ProfileRepository profiles;

    private final CheckerRepository checkers;

    private final SessionStore sessions;

    private final AttemptLimiter attempts;

    private final Clock clock;

    private final ProfilePurgeProperties properties;

    private final Validator validator;

    /**
     * Excluir o perfil encerra as sessões na hora: o access não sobrevive até o
     * TTL do store. O wipe vem antes da exclusão para que store fora do ar vire
     * 503 com o perfil intacto. Os checkers saem no CASCADE do Postgres.
     */
    public void execute(final DeleteProfileCommand command) {
        Constraints.requireValid(validator, command);
        if (!command.id().equals(command.callerProfileId())) {
            throw new ForbiddenProfileDeletionException();
        }
        final var profile = profiles.findById(command.id()).orElseThrow(ProfileNotFoundException::new);
        requireCredentials(command.address(), profile.email());
        if (checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL).isPresent()) {
            throw new EmailNotVerifiedException();
        }
        final var cutoff = clock.instant().minus(Duration.ofMinutes(properties.deleteProfileMaxAgeMinutes()));
        final var checker = checkers.findByProfileIdAndType(profile.id(), Checker.Type.DELETE_PROFILE)
                .filter(current -> !current.createdBefore(cutoff))
                .filter(current -> current.code().equals(command.code()));
        if (checker.isEmpty()) {
            throw InvalidCheckerVerificationException.forCode();
        }
        sessions.wipe(profile.id());
        profiles.deleteById(profile.id());
    }

    private void requireCredentials(final String... keys) {
        Arrays.stream(keys)
                .flatMap(key -> Stream.of(attempts.register(AttemptScope.CREDENTIALS, key)))
                .flatMap(Optional::stream)
                .max(Comparator.naturalOrder())
                .ifPresent(DeleteProfileUseCase::tooMany);
    }

    private static void tooMany(final Duration retryAfter) {
        throw TooManyAttemptsException.forCredentials(retryAfter);
    }

}
