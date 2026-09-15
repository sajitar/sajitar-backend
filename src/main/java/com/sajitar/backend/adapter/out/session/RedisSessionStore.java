package com.sajitar.backend.adapter.out.session;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import com.sajitar.backend.configuration.JwtProperties;
import com.sajitar.backend.domain.exception.SessionStoreUnavailableException;
import com.sajitar.backend.domain.model.token.Session;
import com.sajitar.backend.domain.model.token.TokenClaims;
import com.sajitar.backend.domain.model.token.TokenUse;
import com.sajitar.backend.domain.port.token.RotationCommand;
import com.sajitar.backend.domain.port.token.RotationOutcome;
import com.sajitar.backend.domain.port.token.SessionStore;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
class RedisSessionStore implements SessionStore {

    private static final RedisScript<String> OPEN = script("open.lua");

    private static final RedisScript<String> ACTIVE = script("active.lua");

    private static final RedisScript<String> ROTATE = script("rotate.lua");

    private static final String FIELD_SEPARATOR = "\\|";

    private static final String ROTATED = "rotated";

    private static final String REPLAYED = "replayed";

    private static final String ABSENT = "";

    private final StringRedisTemplate redis;

    private final JwtProperties properties;

    private final Clock clock;

    @Override
    public void open(final Session session, final TokenClaims access, final TokenClaims refresh) {
        final var now = clock.instant();
        final var refreshTtl = refresh == null ? 0L : ttlMillis(refresh, now);
        execute(OPEN, List.of(
                session.id().toString(),
                session.profileId().toString(),
                String.valueOf(session.bornAt().toEpochMilli()),
                access.id().toString(),
                String.valueOf(ttlMillis(access, now)),
                refresh == null ? ABSENT : refresh.id().toString(),
                String.valueOf(refreshTtl),
                String.valueOf(refresh == null ? ttlMillis(access, now) : refreshTtl),
                String.valueOf(properties.maxSessionsPerProfile())));
    }

    @Override
    public Optional<UUID> profileIdOfActiveAccess(final UUID accessId) {
        return active(accessId, TokenUse.ACCESS).map(fields -> UUID.fromString(fields[0]));
    }

    @Override
    public Optional<Session> findActiveRefresh(final UUID refreshId) {
        return active(refreshId, TokenUse.REFRESH).map(fields -> new Session(
                UUID.fromString(fields[1]),
                UUID.fromString(fields[0]),
                UUID.fromString(fields[2]),
                UUID.fromString(fields[3])));
    }

    @Override
    public RotationOutcome rotate(final RotationCommand command) {
        final var now = clock.instant();
        final var session = command.session();
        final var access = command.access();
        final var refresh = command.refresh();
        final var absoluteDeadline = session.bornAt().plusSeconds(properties.sessionMaxSeconds());
        return outcome(execute(ROTATE, List.of(
                command.presentedRefreshId().toString(),
                String.valueOf(now.toEpochMilli()),
                String.valueOf(graceMillis()),
                session.id().toString(),
                access.id().toString(),
                String.valueOf(access.issuedAt().getEpochSecond()),
                String.valueOf(access.expiresAt().getEpochSecond()),
                String.valueOf(ttlMillis(access, now)),
                refresh.id().toString(),
                String.valueOf(refresh.issuedAt().getEpochSecond()),
                String.valueOf(refresh.expiresAt().getEpochSecond()),
                String.valueOf(ttlMillis(refresh, now)),
                String.valueOf(ttlMillis(refresh, now)),
                String.valueOf(Math.max(1L, Duration.between(now, absoluteDeadline).toMillis())))));
    }

    @Override
    public RotationOutcome replay(final UUID refreshId) {
        final var now = clock.instant();
        return outcome(execute(ROTATE, List.of(
                refreshId.toString(),
                String.valueOf(now.toEpochMilli()),
                String.valueOf(graceMillis()))));
    }

    private Optional<String[]> active(final UUID tokenId, final TokenUse use) {
        final var record = execute(ACTIVE, List.of(tokenId.toString(), use.value()));
        return Optional.ofNullable(record).map(value -> value.split(FIELD_SEPARATOR));
    }

    private RotationOutcome outcome(final String result) {
        final var fields = result.split(FIELD_SEPARATOR);
        return switch (fields[0]) {
            case ROTATED -> new RotationOutcome.Rotated();
            case REPLAYED -> new RotationOutcome.Replayed(
                    UUID.fromString(fields[1]),
                    claims(TokenUse.ACCESS, fields, 2),
                    claims(TokenUse.REFRESH, fields, 5));
            default -> new RotationOutcome.Invalid();
        };
    }

    private TokenClaims claims(final TokenUse use, final String[] fields, final int offset) {
        return new TokenClaims(
                UUID.fromString(fields[offset]),
                use,
                Instant.ofEpochSecond(Long.parseLong(fields[offset + 1])),
                Instant.ofEpochSecond(Long.parseLong(fields[offset + 2])));
    }

    private String execute(final RedisScript<String> script, final List<String> args) {
        try {
            return redis.execute(script, List.of(), args.toArray());
        } catch (final DataAccessException _) {
            throw new SessionStoreUnavailableException();
        }
    }

    private long graceMillis() {
        return properties.refreshGraceSeconds() * 1000L;
    }

    private static long ttlMillis(final TokenClaims claims, final Instant now) {
        return Math.max(1L, Duration.between(now, claims.expiresAt()).toMillis());
    }

    private static RedisScript<String> script(final String name) {
        return RedisScript.of(new ClassPathResource("redis/" + name), String.class);
    }

}
