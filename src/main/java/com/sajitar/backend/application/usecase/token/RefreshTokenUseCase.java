package com.sajitar.backend.application.usecase.token;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.command.token.RefreshTokenCommand;
import com.sajitar.backend.domain.exception.EmailNotVerifiedException;
import com.sajitar.backend.domain.exception.InvalidRefreshTokenException;
import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.model.token.IssuedSession;
import com.sajitar.backend.domain.model.token.Session;
import com.sajitar.backend.domain.port.checker.CheckerRepository;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.port.token.RefreshTokenDecoder;
import com.sajitar.backend.domain.port.token.RotationCommand;
import com.sajitar.backend.domain.port.token.RotationOutcome;
import com.sajitar.backend.domain.port.token.SessionStore;
import com.sajitar.backend.domain.port.token.TokenIssuer;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenUseCase {

    private final ProfileRepository profiles;

    private final CheckerRepository checkers;

    private final RefreshTokenDecoder refreshTokens;

    private final TokenIssuer tokens;

    private final SessionStore sessions;

    private final Clock clock;

    private final Validator validator;

    public IssuedSession execute(final RefreshTokenCommand command) {
        Constraints.requireValid(validator, command);
        final var refreshId = refreshTokens.refreshId(command.refreshToken());
        return sessions.findActiveRefresh(refreshId)
                .map(session -> rotate(refreshId, session))
                .orElseGet(() -> consumed(refreshId));
    }

    private IssuedSession rotate(final UUID refreshId, final Session session) {
        final var profile = profiles.findById(session.profileId()).orElseThrow(InvalidRefreshTokenException::new);
        if (checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL).isPresent()) {
            throw new EmailNotVerifiedException();
        }
        final var now = clock.instant();
        final var access = tokens.issueAccess(now);
        final var refresh = tokens.issueRefresh(now, session.bornAt());
        if (refresh.isExpired()) {
            throw new InvalidRefreshTokenException();
        }
        final var command = new RotationCommand(refreshId, session, access.claims(), refresh.claims());
        return switch (sessions.rotate(command)) {
            case RotationOutcome.Rotated _ -> new IssuedSession(session.id(), access, refresh);
            case RotationOutcome.Replayed replayed -> reissue(replayed);
            case RotationOutcome.Invalid _ -> throw new InvalidRefreshTokenException();
        };
    }

    /**
     * Refresh sem registro vigente: dentro da graça reemite o mesmo par sucessor;
     * fora dela o próprio store apaga a sessão e o retry vira 401.
     */
    private IssuedSession consumed(final UUID refreshId) {
        return switch (sessions.replay(refreshId)) {
            case RotationOutcome.Replayed replayed -> reissue(replayed);
            default -> throw new InvalidRefreshTokenException();
        };
    }

    private IssuedSession reissue(final RotationOutcome.Replayed replayed) {
        return new IssuedSession(
                replayed.sessionId(),
                tokens.reissue(replayed.access()),
                tokens.reissue(replayed.refresh()));
    }

}
