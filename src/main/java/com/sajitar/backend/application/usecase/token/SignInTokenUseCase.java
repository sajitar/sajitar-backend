package com.sajitar.backend.application.usecase.token;

import java.time.Clock;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.command.token.SignInTokenCommand;
import com.sajitar.backend.domain.exception.EmailNotVerifiedException;
import com.sajitar.backend.domain.exception.InvalidCredentialsException;
import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.model.token.IssuedSession;
import com.sajitar.backend.domain.model.token.Session;
import com.sajitar.backend.domain.port.PasswordHasher;
import com.sajitar.backend.domain.port.checker.CheckerRepository;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.port.token.SessionStore;
import com.sajitar.backend.domain.port.token.TokenIssuer;

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

    private final Clock clock;

    private final Validator validator;

    public IssuedSession execute(final SignInTokenCommand command) {
        Constraints.requireValid(validator, command);
        final var profile = profiles.findByEmail(command.email()).orElse(null);
        if (profile == null || !passwordHasher.matches(command.password(), profile.password())) {
            throw new InvalidCredentialsException();
        }
        if (checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL).isPresent()) {
            throw new EmailNotVerifiedException();
        }
        final var now = clock.instant();
        final var access = tokens.issueAccess(now);
        final var opened = Session.open(profile.id(), access.id(), null);
        final var refresh = command.refresh() ? tokens.issueRefresh(now, opened.bornAt()) : null;
        final var session = refresh == null ? opened : opened.withRefresh(refresh.id());
        sessions.open(session, access.claims(), refresh == null ? null : refresh.claims());
        return new IssuedSession(session.id(), access, refresh);
    }

}
