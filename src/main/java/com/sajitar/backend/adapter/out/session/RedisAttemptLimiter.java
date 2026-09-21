package com.sajitar.backend.adapter.out.session;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import com.sajitar.backend.configuration.AttemptProperties;
import com.sajitar.backend.domain.exception.SessionStoreUnavailableException;
import com.sajitar.backend.domain.model.token.AttemptScope;
import com.sajitar.backend.domain.port.token.AttemptLimiter;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
class RedisAttemptLimiter implements AttemptLimiter {

    private static final RedisScript<String> ATTEMPT = RedisScript.of(new ClassPathResource("redis/attempt.lua"),
            String.class);

    private static final String ALGORITHM = "SHA-256";

    private final StringRedisTemplate redis;

    private final AttemptProperties properties;

    @Override
    public Optional<Duration> register(final AttemptScope scope, final String key) {
        final var remaining = Objects.requireNonNullElse(execute(List.of(
                "attempt:" + scope.value() + ":" + sha256Hex(ALGORITHM, Objects.requireNonNullElse(key, "")),
                String.valueOf(max(scope)),
                String.valueOf(windowMillis(scope)))), "");
        return remaining.isEmpty() ? Optional.empty() : Optional.of(Duration.ofMillis(Long.parseLong(remaining)));
    }

    private int max(final AttemptScope scope) {
        return switch (scope) {
            case CREDENTIALS -> properties.credentialsMax();
            case REFRESH -> properties.refreshMax();
        };
    }

    private int windowSeconds(final AttemptScope scope) {
        return switch (scope) {
            case CREDENTIALS -> properties.credentialsWindowSeconds();
            case REFRESH -> properties.refreshWindowSeconds();
        };
    }

    private long windowMillis(final AttemptScope scope) {
        return windowSeconds(scope) * 1000L;
    }

    private String execute(final List<String> args) {
        try {
            return redis.execute(ATTEMPT, List.of(), args.toArray());
        } catch (final DataAccessException _) {
            throw new SessionStoreUnavailableException();
        }
    }

    static String sha256Hex(final String algorithm, final String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance(algorithm).digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (final NoSuchAlgorithmException _) {
            throw new SessionStoreUnavailableException();
        }
    }

}
