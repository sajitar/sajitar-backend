package com.sajitar.backend.adapter.out.session;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import com.sajitar.backend.configuration.JwtProperties;
import com.sajitar.backend.domain.exception.SessionStoreUnavailableException;
import com.sajitar.backend.domain.model.token.ActiveSession;
import com.sajitar.backend.domain.model.token.Client;
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

    private static final RedisScript<String> LIST = script("list.lua");

    private static final RedisScript<String> CLOSE = script("close.lua");

    private static final RedisScript<String> WIPE = script("wipe.lua");

    private static final String FIELD_SEPARATOR = "\\|";

    private static final String RECORD_SEPARATOR = "\n";

    private static final String ROTATED = "rotated";

    private static final String REPLAYED = "replayed";

    private static final String CLOSED = "closed";

    private static final String ABSENT = "";

    private final StringRedisTemplate redis;

    private final JwtProperties properties;

    private final Clock clock;

    @Override
    public void open(final Session session, final TokenClaims access, final TokenClaims refresh, final Client client) {
        final var now = clock.instant();
        final var refreshTtl = refresh == null ? 0L : ttlMillis(refresh, now);
        execute(OPEN, withClient(List.of(
                session.id().toString(),
                session.profileId().toString(),
                String.valueOf(session.bornAt().toEpochMilli()),
                access.id().toString(),
                String.valueOf(ttlMillis(access, now)),
                refresh == null ? ABSENT : refresh.id().toString(),
                String.valueOf(refreshTtl),
                String.valueOf(refresh == null ? ttlMillis(access, now) : refreshTtl),
                String.valueOf(properties.maxSessionsPerProfile())), client));
    }

    @Override
    public Optional<Session> findActiveAccess(final UUID accessId) {
        return active(accessId, TokenUse.ACCESS).map(RedisSessionStore::session);
    }

    @Override
    public Optional<Session> findActiveRefresh(final UUID refreshId) {
        return active(refreshId, TokenUse.REFRESH).map(RedisSessionStore::session);
    }

    @Override
    public List<ActiveSession> activeSessions(final UUID profileId) {
        final var members = execute(LIST, List.of(profileId.toString()));
        return members.isEmpty()
                ? List.of()
                : Stream.of(members.split(RECORD_SEPARATOR)).map(RedisSessionStore::activeSession).toList();
    }

    @Override
    public boolean close(final UUID profileId, final List<UUID> sessionIds) {
        final var args = Stream.concat(Stream.of(profileId), sessionIds.stream()).map(UUID::toString).toList();
        return CLOSED.equals(execute(CLOSE, args));
    }

    @Override
    public void wipe(final UUID profileId) {
        execute(WIPE, List.of(profileId.toString()));
    }

    @Override
    public RotationOutcome rotate(final RotationCommand command) {
        final var now = clock.instant();
        final var session = command.session();
        final var access = command.access();
        final var refresh = command.refresh();
        final var absoluteDeadline = session.bornAt().plusSeconds(properties.sessionMaxSeconds());
        return outcome(execute(ROTATE, withClient(List.of(
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
                String.valueOf(Math.max(1L, Duration.between(now, absoluteDeadline).toMillis()))), command.client())));
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
        return Optional.ofNullable(record).map(value -> value.split(FIELD_SEPARATOR, -1));
    }

    /** Sessão do registro devolvido pelo {@code active.lua}; sem refresh vem vazio. */
    private static Session session(final String[] fields) {
        return new Session(
                UUID.fromString(fields[1]),
                UUID.fromString(fields[0]),
                UUID.fromString(fields[2]),
                ABSENT.equals(fields[3]) ? null : UUID.fromString(fields[3]));
    }

    private static ActiveSession activeSession(final String record) {
        final var fields = record.split(FIELD_SEPARATOR, -1);
        return new ActiveSession(UUID.fromString(fields[0]), client(fields));
    }

    private static Client client(final String[] fields) {
        final var name = field(fields, 1);
        final var os = field(fields, 2);
        final var device = field(fields, 3);
        if (name == null && os == null && device == null) {
            return null;
        }
        return new Client(name, os, device == null ? Client.Device.UNKNOWN : Client.Device.of(device));
    }

    private static String field(final String[] fields, final int index) {
        return fields[index].isEmpty() ? null : fields[index];
    }

    private static List<String> withClient(final List<String> args, final Client client) {
        final var complete = new ArrayList<>(args);
        complete.add(client == null || client.name() == null ? ABSENT : client.name());
        complete.add(client == null || client.os() == null ? ABSENT : client.os());
        complete.add(client == null || client.device() == null ? ABSENT : client.device().value());
        return complete;
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
