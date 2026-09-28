package com.sajitar.backend.application.usecase.profile;

import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sajitar.backend.application.ChangeEmailMail;
import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.command.profile.RequestChangeEmailCommand;
import com.sajitar.backend.configuration.ProfilePurgeProperties;
import com.sajitar.backend.domain.exception.EmailNotVerifiedException;
import com.sajitar.backend.domain.exception.InvalidCheckerVerificationException;
import com.sajitar.backend.domain.exception.ProfileNotFoundException;
import com.sajitar.backend.domain.exception.TooManyAttemptsException;
import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.model.token.AttemptScope;
import com.sajitar.backend.domain.port.Mailer;
import com.sajitar.backend.domain.port.checker.CheckerRepository;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.port.token.AttemptLimiter;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RequestChangeEmailUseCase {

    private final ProfileRepository profiles;

    private final CheckerRepository checkers;

    private final Mailer mailer;

    private final AttemptLimiter attempts;

    private final Clock clock;

    private final ProfilePurgeProperties properties;

    private final MessageSource messageSource;

    private final Validator validator;

    @Transactional
    public void execute(final RequestChangeEmailCommand command) {
        Constraints.requireValid(validator, command);
        final var profile = profiles.findById(command.profileId()).orElseThrow(ProfileNotFoundException::new);
        requireCredentials(command.address(), profile.email());
        if (checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL).isPresent()) {
            throw new EmailNotVerifiedException();
        }
        final var cutoff = clock.instant().minus(Duration.ofHours(properties.changeEmailMaxAgeHours()));
        final var existing = checkers.findByProfileIdAndType(profile.id(), Checker.Type.CHANGE_EMAIL);
        if (existing.filter(checker -> checker.createdBefore(cutoff)).isPresent()) {
            throw InvalidCheckerVerificationException.forCode();
        }
        final var checker = existing
                .map(current -> current.rotate(current.type(), current.payload()))
                .orElseGet(() -> Checker.create(profile.id(), Checker.Type.CHANGE_EMAIL));
        final var saved = checkers.save(checker);
        final var to = saved.payload() == null ? profile.email() : saved.payload();
        final var hours = properties.changeEmailMaxAgeHours();
        mailer.send(saved.payload() == null
                ? ChangeEmailMail.composeRecovery(messageSource, clock.instant(), to, saved.code(), hours)
                : ChangeEmailMail.composeConfirm(messageSource, clock.instant(), to, saved.code(), hours));
    }

    private void requireCredentials(final String... keys) {
        Arrays.stream(keys)
                .flatMap(key -> Stream.of(attempts.register(AttemptScope.CREDENTIALS, key)))
                .flatMap(Optional::stream)
                .max(Comparator.naturalOrder())
                .ifPresent(RequestChangeEmailUseCase::tooMany);
    }

    private static void tooMany(final Duration retryAfter) {
        throw TooManyAttemptsException.forCredentials(retryAfter);
    }

}
