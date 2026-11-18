package com.sajitar.backend.application.usecase.token;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.command.token.RefreshTokenCommand;
import com.sajitar.backend.domain.exception.EmailNotVerifiedException;
import com.sajitar.backend.domain.exception.InvalidRefreshTokenException;
import com.sajitar.backend.domain.exception.TermNotAcceptedException;
import com.sajitar.backend.domain.exception.TooManyAttemptsException;
import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.model.profile.Profile;
import com.sajitar.backend.domain.model.term.Term;
import com.sajitar.backend.domain.model.token.AttemptScope;
import com.sajitar.backend.domain.model.token.IssuedSession;
import com.sajitar.backend.domain.model.token.Session;
import com.sajitar.backend.domain.port.checker.CheckerRepository;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.port.term.TermRepository;
import com.sajitar.backend.domain.port.token.AttemptLimiter;
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

    private final TermRepository terms;

    private final RefreshTokenDecoder refreshTokens;

    private final TokenIssuer tokens;

    private final SessionStore sessions;

    private final AttemptLimiter attempts;

    private final Clock clock;

    private final Validator validator;

    @Transactional
    public IssuedSession execute(final RefreshTokenCommand command) {
        Constraints.requireValid(validator, command);
        attempts.register(AttemptScope.REFRESH, command.address())
                .ifPresent(retryAfter -> {
                    throw TooManyAttemptsException.forRefreshToken(retryAfter);
                });
        final var refreshId = refreshTokens.refreshId(command.refreshToken());
        return sessions.findActiveRefresh(refreshId)
                .map(session -> rotate(refreshId, session, command))
                .orElseGet(() -> consumed(refreshId, command));
    }

    private IssuedSession rotate(final UUID refreshId, final Session session, final RefreshTokenCommand command) {
        final var profile = profiles.findById(session.profileId()).orElseThrow(InvalidRefreshTokenException::new);
        if (checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL).isPresent()) {
            throw new EmailNotVerifiedException();
        }
        final var current = terms.findCurrent();
        final var needsAcceptance = needsTermAcceptance(profile, current);
        requireTermAccepted(needsAcceptance, current, command.termAccepted());
        final var now = clock.instant();
        final var access = tokens.issueAccess(now);
        final var refresh = tokens.issueRefresh(now, session.bornAt());
        if (refresh.isExpired()) {
            throw new InvalidRefreshTokenException();
        }
        final var rotation = new RotationCommand(refreshId, session, access.claims(), refresh.claims(), command.client());
        return switch (sessions.rotate(rotation)) {
            case RotationOutcome.Rotated _ -> {
                acceptTerm(profile, current, needsAcceptance);
                yield new IssuedSession(session.id(), access, refresh);
            }
            case RotationOutcome.Replayed replayed -> {
                acceptTerm(profile, current, needsAcceptance);
                yield reissue(replayed);
            }
            case RotationOutcome.Invalid _ -> throw new InvalidRefreshTokenException();
        };
    }

    /**
     * Refresh sem registro vigente: dentro da graça reemite o mesmo par sucessor;
     * fora dela o próprio store apaga a sessão e o retry vira 401.
     */
    private IssuedSession consumed(final UUID refreshId, final RefreshTokenCommand command) {
        return switch (sessions.replay(refreshId)) {
            case RotationOutcome.Replayed replayed -> {
                final var session = sessions.find(replayed.sessionId()).orElseThrow(InvalidRefreshTokenException::new);
                final var profile = profiles.findById(session.profileId()).orElseThrow(InvalidRefreshTokenException::new);
                final var current = terms.findCurrent();
                final var needsAcceptance = needsTermAcceptance(profile, current);
                requireTermAccepted(needsAcceptance, current, command.termAccepted());
                acceptTerm(profile, current, needsAcceptance);
                yield reissue(replayed);
            }
            default -> throw new InvalidRefreshTokenException();
        };
    }

    private void acceptTerm(final Profile profile, final Optional<Term> current, final boolean needsAcceptance) {
        if (needsAcceptance) {
            profiles.save(profile.withTermId(current.get().id()));
        }
    }

    private static void requireTermAccepted(
            final boolean needsAcceptance,
            final Optional<Term> current,
            final boolean termAccepted) {
        if (needsAcceptance && !termAccepted) {
            throw new TermNotAcceptedException();
        }
        if (current.isEmpty()) {
            throw new TermNotAcceptedException();
        }
    }

    private static boolean needsTermAcceptance(final Profile profile, final Optional<Term> current) {
        return current.isEmpty() || !Objects.equals(profile.termId(), current.get().id());
    }

    private IssuedSession reissue(final RotationOutcome.Replayed replayed) {
        return new IssuedSession(
                replayed.sessionId(),
                tokens.reissue(replayed.access()),
                tokens.reissue(replayed.refresh()));
    }

}
