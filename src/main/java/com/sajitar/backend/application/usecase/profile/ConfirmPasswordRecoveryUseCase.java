package com.sajitar.backend.application.usecase.profile;

import java.time.Clock;
import java.time.Duration;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.command.profile.ConfirmPasswordRecoveryCommand;
import com.sajitar.backend.configuration.ProfilePurgeProperties;
import com.sajitar.backend.domain.exception.InvalidCheckerVerificationException;
import com.sajitar.backend.domain.exception.TooManyAttemptsException;
import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.model.profile.Profile;
import com.sajitar.backend.domain.model.token.AttemptScope;
import com.sajitar.backend.domain.port.PasswordHasher;
import com.sajitar.backend.domain.port.checker.CheckerRepository;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.port.token.AttemptLimiter;
import com.sajitar.backend.domain.port.token.SessionStore;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConfirmPasswordRecoveryUseCase {

    private final ProfileRepository profiles;

    private final CheckerRepository checkers;

    private final PasswordHasher passwordHasher;

    private final SessionStore sessions;

    private final AttemptLimiter attempts;

    private final Clock clock;

    private final ProfilePurgeProperties properties;

    private final Validator validator;

    /**
     * Confirma a recuperação: o store é tocado antes da escrita. Fora do ar
     * vira 503 sem trocar a senha, nunca o contrário.
     */
    public void execute(final ConfirmPasswordRecoveryCommand command) {
        Constraints.requireValid(validator, command);
        requireCredentials(command.address(), command.email());
        final var profile = profiles.findByEmail(command.email()).orElse(null);
        final var cutoff = clock.instant().minus(Duration.ofHours(properties.changePasswordMaxAgeHours()));
        final var checker = profile == null
                ? Optional.<Checker>empty()
                : checkers.findByProfileIdAndType(profile.id(), Checker.Type.CHANGE_PASSWORD)
                        .filter(current -> !current.createdBefore(cutoff));
        if (profile == null || checker.isEmpty() || !checker.get().code().equals(command.code())) {
            throw InvalidCheckerVerificationException.forCode();
        }
        sessions.wipe(profile.id());
        final var hashed = passwordHasher.hash(command.newPassword());
        profiles.save(new Profile(
                profile.id(),
                profile.name(),
                profile.description(),
                profile.birthday(),
                profile.email(),
                hashed));
        checkers.deleteById(checker.get().id());
    }

    private void requireCredentials(final String address, final String email) {
        Stream.of(
                attempts.register(AttemptScope.CREDENTIALS, address),
                attempts.register(AttemptScope.CREDENTIALS, email))
                .flatMap(Optional::stream)
                .max(Comparator.naturalOrder())
                .ifPresent(ConfirmPasswordRecoveryUseCase::tooMany);
    }

    private static void tooMany(final Duration retryAfter) {
        throw TooManyAttemptsException.forCredentials(retryAfter);
    }

}
