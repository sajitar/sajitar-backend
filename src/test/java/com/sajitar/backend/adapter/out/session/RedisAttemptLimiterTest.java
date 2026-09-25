package com.sajitar.backend.adapter.out.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.sajitar.backend.configuration.AttemptPropertiesFixture;
import com.sajitar.backend.domain.exception.SessionStoreUnavailableException;
import com.sajitar.backend.domain.model.token.AttemptScope;
import com.sajitar.backend.settlement.token.SessionSettlementFixture;

@DisplayName("RedisAttemptLimiter (integração Redis)")
class RedisAttemptLimiterTest {

    private static final String HOST = environment("SPRING_DATA_REDIS_HOST", "127.0.0.1");

    private static final int PORT = Integer.parseInt(environment("SPRING_DATA_REDIS_PORT", "6379"));

    private static final String USERNAME = environment("SPRING_DATA_REDIS_USERNAME", "sajitar");

    private static final String PASSWORD = environment("SPRING_DATA_REDIS_PASSWORD", "sajitar_dev");

    private static LettuceConnectionFactory connectionFactory;

    private static StringRedisTemplate redis;

    private final RedisAttemptLimiter limiter = new RedisAttemptLimiter(redis, AttemptPropertiesFixture.tight());

    @BeforeAll
    static void connect() {
        connectionFactory = connectionFactory(PORT);
        redis = new StringRedisTemplate(connectionFactory);
        redis.afterPropertiesSet();
    }

    @AfterAll
    static void disconnect() {
        connectionFactory.destroy();
    }

    @BeforeEach
    void clearAttempts() {
        SessionSettlementFixture.clear(redis);
    }

    @Test
    @DisplayName("Dentro do teto a tentativa cabe; a seguinte devolve a espera")
    void allowsUntilMaxThenReturnsRetryAfter() {
        assertThat(limiter.register(AttemptScope.CREDENTIALS, "203.0.113.10")).isEmpty();
        assertThat(limiter.register(AttemptScope.CREDENTIALS, "203.0.113.10")).isEmpty();

        final var retryAfter = limiter.register(AttemptScope.CREDENTIALS, "203.0.113.10");

        assertThat(retryAfter).isPresent();
        assertThat(retryAfter.orElseThrow().toMillis()).isPositive();
    }

    @Test
    @DisplayName("Escopos e chaves diferentes não compartilham o contador")
    void scopesAndKeysAreIndependent() {
        limiter.register(AttemptScope.CREDENTIALS, "address-a");
        limiter.register(AttemptScope.CREDENTIALS, "address-a");
        limiter.register(AttemptScope.CREDENTIALS, "address-a");

        assertThat(limiter.register(AttemptScope.CREDENTIALS, "address-b")).isEmpty();
        assertThat(limiter.register(AttemptScope.REFRESH, "address-a")).isEmpty();
        assertThat(limiter.register(AttemptScope.REFRESH, "address-a")).isEmpty();
        assertThat(limiter.register(AttemptScope.REFRESH, "address-a")).isPresent();
    }

    @Test
    @DisplayName("Chave nula é hasheada como vazia e ainda conta")
    void hashesNullKeyAsEmpty() {
        assertThat(limiter.register(AttemptScope.CREDENTIALS, null)).isEmpty();
        assertThat(limiter.register(AttemptScope.CREDENTIALS, "")).isEmpty();
        assertThat(limiter.register(AttemptScope.CREDENTIALS, null)).isPresent();
    }

    @Test
    @DisplayName("Redis fora do ar vira indisponibilidade do store, não 401")
    void failsClosedWhenRedisIsDown() {
        final var offline = new LettuceConnectionFactory(new RedisStandaloneConfiguration(HOST, PORT + 20));
        offline.afterPropertiesSet();
        final var offlineTemplate = new StringRedisTemplate(offline);
        offlineTemplate.afterPropertiesSet();
        final var offlineLimiter = new RedisAttemptLimiter(offlineTemplate, AttemptPropertiesFixture.tight());

        final var thrown = catchThrowable(() -> offlineLimiter.register(AttemptScope.REFRESH, "203.0.113.10"));

        assertThat(thrown).isInstanceOf(SessionStoreUnavailableException.class);
        offline.destroy();
    }

    @Test
    @DisplayName("Algoritmo desconhecido na chave hasheada também falha fechado")
    void failsClosedWhenDigestIsUnavailable() {
        final var thrown = catchThrowable(() -> RedisAttemptLimiter.sha256Hex("not-a-digest", "key"));

        assertThat(thrown).isInstanceOf(SessionStoreUnavailableException.class);
    }

    private static String environment(final String name, final String fallback) {
        final var value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static LettuceConnectionFactory connectionFactory(final int port) {
        final var configuration = new RedisStandaloneConfiguration(HOST, port);
        configuration.setUsername(USERNAME);
        configuration.setPassword(PASSWORD);
        final var factory = new LettuceConnectionFactory(configuration);
        factory.afterPropertiesSet();
        return factory;
    }

}
