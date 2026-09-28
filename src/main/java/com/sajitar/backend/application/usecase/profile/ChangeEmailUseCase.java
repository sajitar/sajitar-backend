package com.sajitar.backend.application.usecase.profile;

import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.command.profile.ChangeEmailCommand;
import com.sajitar.backend.configuration.ProfilePurgeProperties;
import com.sajitar.backend.domain.exception.EmailAlreadyRegisteredException;
import com.sajitar.backend.domain.exception.EmailNotVerifiedException;
import com.sajitar.backend.domain.exception.InvalidCheckerVerificationException;
import com.sajitar.backend.domain.exception.ProfileNotFoundException;
import com.sajitar.backend.domain.exception.TooManyAttemptsException;
import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.model.profile.Profile;
import com.sajitar.backend.domain.model.token.AttemptScope;
import com.sajitar.backend.domain.port.checker.CheckerRepository;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.port.token.AttemptLimiter;
import com.sajitar.backend.domain.port.token.SessionStore;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChangeEmailUseCase {

    private final ProfileRepository profiles;

    private final CheckerRepository checkers;

    private final SessionStore sessions;

    private final AttemptLimiter attempts;

    private final Clock clock;

    private final ProfilePurgeProperties properties;

    private final Validator validator;

    /**
     * Troca o e-mail do perfil da sessão. O store é tocado antes da escrita:
     * fora do ar vira 503 sem gravar o e-mail, nunca o contrário.
     */
    public void execute(final ChangeEmailCommand command) {
        Constraints.requireValid(validator, command);
        final var profile = profiles.findById(command.profileId()).orElseThrow(ProfileNotFoundException::new);
        requireCredentials(command.address(), profile.email());
        if (checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL).isPresent()) {
            throw new EmailNotVerifiedException();
        }
        final var cutoff = clock.instant().minus(Duration.ofHours(properties.changeEmailMaxAgeHours()));
        final var checker = checkers.findByProfileIdAndType(profile.id(), Checker.Type.CHANGE_EMAIL)
                .filter(current -> !current.createdBefore(cutoff))
                .filter(current -> current.payload() != null)
                .filter(current -> current.code().equals(command.code()));
        if (checker.isEmpty()) {
            throw InvalidCheckerVerificationException.forCode();
        }
        final var found = checker.get();
        if (profiles.findByEmail(found.payload()).isPresent()) {
            throw new EmailAlreadyRegisteredException();
        }
        sessions.wipe(profile.id());
        profiles.save(new Profile(
                profile.id(),
                profile.name(),
                profile.description(),
                profile.birthday(),
                found.payload(),
                profile.password()));
        checkers.deleteById(found.id());
    }

    private void requireCredentials(final String... keys) {
        Arrays.stream(keys)
                .flatMap(key -> Stream.of(attempts.register(AttemptScope.CREDENTIALS, key)))
                .flatMap(Optional::stream)
                .max(Comparator.naturalOrder())
                .ifPresent(ChangeEmailUseCase::tooMany);
    }

    private static void tooMany(final Duration retryAfter) {
        throw TooManyAttemptsException.forCredentials(retryAfter);
    }

}
