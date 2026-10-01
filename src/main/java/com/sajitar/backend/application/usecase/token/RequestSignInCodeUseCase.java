package com.sajitar.backend.application.usecase.token;

import java.time.Clock;
import java.time.Duration;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.SignInMail;
import com.sajitar.backend.application.command.token.RequestSignInCodeCommand;
import com.sajitar.backend.configuration.ProfilePurgeProperties;
import com.sajitar.backend.domain.exception.InvalidCredentialsException;
import com.sajitar.backend.domain.exception.TooManyAttemptsException;
import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.model.token.AttemptScope;
import com.sajitar.backend.domain.port.Mailer;
import com.sajitar.backend.domain.port.PasswordHasher;
import com.sajitar.backend.domain.port.checker.CheckerRepository;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.port.token.AttemptLimiter;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RequestSignInCodeUseCase {

    private final ProfileRepository profiles;

    private final CheckerRepository checkers;

    private final PasswordHasher passwordHasher;

    private final Mailer mailer;

    private final AttemptLimiter attempts;

    private final Clock clock;

    private final ProfilePurgeProperties properties;

    private final MessageSource messageSource;

    private final Validator validator;

    @Transactional
    public void execute(final RequestSignInCodeCommand command) {
        Constraints.requireValid(validator, command);
        requireCredentials(command.address(), command.email());
        final var profile = profiles.findByEmail(command.email()).orElse(null);
        if (profile == null || !passwordHasher.matches(command.password(), profile.password())) {
            throw new InvalidCredentialsException();
        }
        if (checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL).isPresent()) {
            return;
        }
        if (!profile.requiresTwoFactor()) {
            return;
        }
        final var cutoff = clock.instant().minus(Duration.ofMinutes(properties.signInMaxAgeMinutes()));
        final var existing = checkers.findByProfileIdAndType(profile.id(), Checker.Type.SIGN_IN);
        if (existing.filter(checker -> checker.createdBefore(cutoff)).isPresent()) {
            return;
        }
        final var checker = existing
                .map(current -> current.rotate(current.type(), current.payload()))
                .orElseGet(() -> Checker.create(profile.id(), Checker.Type.SIGN_IN));
        final var saved = checkers.save(checker);
        mailer.send(SignInMail.compose(
                messageSource,
                clock.instant(),
                profile.email(),
                saved.code(),
                properties.signInMaxAgeMinutes()));
    }

    private void requireCredentials(final String address, final String email) {
        Stream.of(
                attempts.register(AttemptScope.CREDENTIALS, address),
                attempts.register(AttemptScope.CREDENTIALS, email))
                .flatMap(Optional::stream)
                .max(Comparator.naturalOrder())
                .ifPresent(RequestSignInCodeUseCase::tooMany);
    }

    private static void tooMany(final Duration retryAfter) {
        throw TooManyAttemptsException.forCredentials(retryAfter);
    }

}
