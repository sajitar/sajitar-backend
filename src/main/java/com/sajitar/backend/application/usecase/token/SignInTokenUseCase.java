package com.sajitar.backend.application.usecase.token;

import java.time.Clock;
import java.time.Duration;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.command.token.SignInTokenCommand;
import com.sajitar.backend.domain.exception.EmailNotVerifiedException;
import com.sajitar.backend.domain.exception.InvalidCheckerVerificationException;
import com.sajitar.backend.domain.exception.InvalidCredentialsException;
import com.sajitar.backend.domain.exception.TooManyAttemptsException;
import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.model.token.AttemptScope;
import com.sajitar.backend.domain.model.token.IssuedSession;
import com.sajitar.backend.domain.model.token.Session;
import com.sajitar.backend.domain.port.PasswordHasher;
import com.sajitar.backend.domain.port.checker.CheckerRepository;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.port.token.AttemptLimiter;
import com.sajitar.backend.domain.port.token.SessionStore;
import com.sajitar.backend.domain.port.token.TokenIssuer;
import com.sajitar.backend.domain.validation.checker.Code;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SignInTokenUseCase {

    private final ProfileRepository profiles;

    private final CheckerRepository checkers;

    private final PasswordHasher passwordHasher;

    private final TokenIssuer tokens;

    private final SessionStore sessions;

    private final AttemptLimiter attempts;

    private final Clock clock;

    private final Validator validator;

    @Transactional
    public IssuedSession execute(final SignInTokenCommand command) {
        Constraints.requireValid(validator, command);
        requireCredentials(command.address(), command.email());
        final var profile = profiles.findByEmail(command.email()).orElse(null);
        if (profile == null || !passwordHasher.matches(command.password(), profile.password())) {
            throw new InvalidCredentialsException();
        }
        checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL)
                .ifPresent(checker -> consumeVerifyEmail(checker, command.code()));
        final var now = clock.instant();
        final var access = tokens.issueAccess(now);
        final var opened = Session.open(profile.id(), access.id(), null);
        final var refresh = command.refresh() ? tokens.issueRefresh(now, opened.bornAt()) : null;
        final var session = refresh == null ? opened : opened.withRefresh(refresh.id());
        sessions.open(session, access.claims(), refresh == null ? null : refresh.claims(), command.client());
        return new IssuedSession(session.id(), access, refresh);
    }

    private void consumeVerifyEmail(final Checker checker, final String code) {
        if (code == null || code.isBlank()) {
            throw new EmailNotVerifiedException();
        }
        Code.Validation.validate(validator, code);
        if (!checker.code().equals(code)) {
            throw InvalidCheckerVerificationException.forCode();
        }
        checkers.deleteById(checker.id());
    }

    private void requireCredentials(final String address, final String email) {
        Stream.of(
                attempts.register(AttemptScope.CREDENTIALS, address),
                attempts.register(AttemptScope.CREDENTIALS, email))
                .flatMap(Optional::stream)
                .max(Comparator.naturalOrder())
                .ifPresent(SignInTokenUseCase::tooMany);
    }

    private static void tooMany(final Duration retryAfter) {
        throw TooManyAttemptsException.forCredentials(retryAfter);
    }

}
