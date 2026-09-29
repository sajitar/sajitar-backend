package com.sajitar.backend.adapter.out.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.sajitar.backend.configuration.AttemptPropertiesFixture;
import com.sajitar.backend.domain.exception.SessionStoreUnavailableException;
import com.sajitar.backend.domain.model.token.AttemptScope;
import com.sajitar.backend.settlement.token.SessionSettlementFixture;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;

@ExtendWith(OutputCaptureExtension.class)
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
        routeErrorLogToStdout(RedisAttemptLimiter.class);
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
    void failsClosedWhenRedisIsDown(final CapturedOutput output) {
        final var offline = new LettuceConnectionFactory(new RedisStandaloneConfiguration(HOST, PORT + 20));
        offline.afterPropertiesSet();
        final var offlineTemplate = new StringRedisTemplate(offline);
        offlineTemplate.afterPropertiesSet();
        final var offlineLimiter = new RedisAttemptLimiter(offlineTemplate, AttemptPropertiesFixture.tight());

        final var thrown = catchThrowable(() -> offlineLimiter.register(AttemptScope.REFRESH, "203.0.113.10"));

        assertThat(thrown).isInstanceOf(SessionStoreUnavailableException.class);
        assertThat(output).contains("Session store unavailable");
        offline.destroy();
    }

    @Test
    @DisplayName("Algoritmo desconhecido na chave hasheada também falha fechado")
    void failsClosedWhenDigestIsUnavailable(final CapturedOutput output) {
        final var thrown = catchThrowable(() -> RedisAttemptLimiter.sha256Hex("not-a-digest", "key"));

        assertThat(thrown).isInstanceOf(SessionStoreUnavailableException.class);
        assertThat(output).contains("Session store unavailable", "not-a-digest");
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

    private static void routeErrorLogToStdout(final Class<?> type) {
        if (!(org.slf4j.LoggerFactory.getILoggerFactory() instanceof LoggerContext context)) {
            return;
        }
        final var logger = context.getLogger(type);
        logger.detachAndStopAllAppenders();
        final var encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern("%msg%n%ex");
        encoder.start();
        final var appender = new ConsoleAppender<ILoggingEvent>();
        appender.setContext(context);
        appender.setEncoder(encoder);
        appender.setTarget("System.out");
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.ERROR);
        logger.setAdditive(false);
    }

}
