package com.sajitar.backend.adapter.out.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.sajitar.backend.configuration.JwtProperties;
import com.sajitar.backend.configuration.JwtPropertiesFixture;
import com.sajitar.backend.domain.exception.SessionStoreUnavailableException;
import com.sajitar.backend.domain.model.token.Session;
import com.sajitar.backend.domain.model.token.TokenClaims;
import com.sajitar.backend.domain.model.token.TokenUse;
import com.sajitar.backend.domain.port.token.RotationCommand;
import com.sajitar.backend.domain.port.token.RotationOutcome;
import com.sajitar.backend.settlement.token.SessionSettlementFixture;

/**
 * Exercita os scripts Lua contra o Redis do ambiente de testes (mesmas
 * credenciais de {@code docker/redis/users.acl}).
 */
@DisplayName("RedisSessionStore (integração Redis)")
class RedisSessionStoreTest {

    private static final Clock CLOCK = Clock.systemUTC();

    private static final String HOST = environment("SPRING_DATA_REDIS_HOST", "127.0.0.1");

    private static final int PORT = Integer.parseInt(environment("SPRING_DATA_REDIS_PORT", "6379"));

    private static final String USERNAME = environment("SPRING_DATA_REDIS_USERNAME", "sajitar");

    private static final String PASSWORD = environment("SPRING_DATA_REDIS_PASSWORD", "sajitar_dev");

    private static LettuceConnectionFactory connectionFactory;

    private static StringRedisTemplate redis;

    private final RedisSessionStore store = store(JwtPropertiesFixture.defaults());

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
    void clearSessions() {
        SessionSettlementFixture.clear(redis);
    }

    @Test
    @DisplayName("Signin sem refresh grava só o access, que autentica pelo jti")
    void opensSessionWithAccessOnly() {
        final var profileId = UUID.randomUUID();
        final var access = accessClaims();
        final var session = Session.open(profileId, access.id(), null);

        store.open(session, access, null);

        assertThat(profileOfAccess(store, access.id())).contains(profileId);
        assertThat(store.findActiveRefresh(access.id())).isEmpty();
        assertThat(redis.getExpire("token:" + access.id())).isPositive();
        assertThat(redis.getExpire("session:" + session.id())).isPositive();
    }

    @Test
    @DisplayName("Signin com refresh liga os dois jti na mesma sessão")
    void opensSessionWithPair() {
        final var profileId = UUID.randomUUID();
        final var access = accessClaims();
        final var refresh = refreshClaims();
        final var session = Session.open(profileId, access.id(), refresh.id());

        store.open(session, access, refresh);

        assertThat(profileOfAccess(store, access.id())).contains(profileId);
        assertThat(store.findActiveRefresh(refresh.id())).contains(session);
    }

    @Test
    @DisplayName("Token sem registro, de outro uso ou órfão não autentica")
    void rejectsUnknownAndForeignTokens() {
        final var access = accessClaims();
        final var refresh = refreshClaims();
        final var session = Session.open(UUID.randomUUID(), access.id(), refresh.id());
        store.open(session, access, refresh);

        assertThat(profileOfAccess(store, UUID.randomUUID())).isEmpty();
        assertThat(profileOfAccess(store, refresh.id())).isEmpty();
        assertThat(store.findActiveRefresh(access.id())).isEmpty();
    }

    @Test
    @DisplayName("Ao estourar o teto, a sessão mais antiga do perfil é encerrada")
    void evictsOldestSessionOverLimit() throws InterruptedException {
        final var limited = store(JwtPropertiesFixture.with(
                JwtPropertiesFixture.SESSION_MAX_SECONDS,
                2,
                JwtPropertiesFixture.REFRESH_GRACE_SECONDS));
        final var profileId = UUID.randomUUID();

        final var first = open(limited, profileId);
        Thread.sleep(2);
        final var second = open(limited, profileId);
        Thread.sleep(2);
        final var third = open(limited, profileId);

        assertThat(profileOfAccess(limited, first.accessId())).isEmpty();
        assertThat(profileOfAccess(limited, second.accessId())).contains(profileId);
        assertThat(profileOfAccess(limited, third.accessId())).contains(profileId);
    }

    @Test
    @DisplayName("Sessão morta por TTL sai do índice do perfil e não ocupa o teto")
    void dropsDeadSessionsFromProfileIndex() {
        final var limited = store(JwtPropertiesFixture.with(
                JwtPropertiesFixture.SESSION_MAX_SECONDS,
                2,
                JwtPropertiesFixture.REFRESH_GRACE_SECONDS));
        final var profileId = UUID.randomUUID();
        final var first = open(limited, profileId);
        final var second = open(limited, profileId);
        redis.unlink("session:" + first.id());

        final var third = open(limited, profileId);

        assertThat(profileOfAccess(limited, second.accessId())).contains(profileId);
        assertThat(profileOfAccess(limited, third.accessId())).contains(profileId);
    }

    @Test
    @DisplayName("Rotação troca o par vigente e derruba o access ligado ao refresh consumido")
    void rotatesCurrentPair() {
        final var profileId = UUID.randomUUID();
        final var access = accessClaims();
        final var refresh = refreshClaims();
        final var session = Session.open(profileId, access.id(), refresh.id());
        store.open(session, access, refresh);
        final var nextAccess = accessClaims();
        final var nextRefresh = refreshClaims();

        final var outcome = store.rotate(new RotationCommand(refresh.id(), session, nextAccess, nextRefresh));

        assertThat(outcome).isInstanceOf(RotationOutcome.Rotated.class);
        assertThat(profileOfAccess(store, access.id())).isEmpty();
        assertThat(store.findActiveRefresh(refresh.id())).isEmpty();
        assertThat(profileOfAccess(store, nextAccess.id())).contains(profileId);
        assertThat(store.findActiveRefresh(nextRefresh.id()))
                .map(Session::id)
                .contains(session.id());
    }

    @Test
    @DisplayName("Retry dentro da graça devolve as claims do par sucessor sem gravar de novo")
    void replaysSuccessorWithinGrace() {
        final var profileId = UUID.randomUUID();
        final var access = accessClaims();
        final var refresh = refreshClaims();
        final var session = Session.open(profileId, access.id(), refresh.id());
        store.open(session, access, refresh);
        final var nextAccess = accessClaims();
        final var nextRefresh = refreshClaims();
        store.rotate(new RotationCommand(refresh.id(), session, nextAccess, nextRefresh));

        final var outcome = store.replay(refresh.id());

        assertThat(outcome).isInstanceOf(RotationOutcome.Replayed.class);
        final var replayed = (RotationOutcome.Replayed) outcome;
        assertThat(replayed.sessionId()).isEqualTo(session.id());
        assertThat(replayed.access()).isEqualTo(nextAccess);
        assertThat(replayed.refresh()).isEqualTo(nextRefresh);
        assertThat(profileOfAccess(store, nextAccess.id())).contains(profileId);
    }

    @Test
    @DisplayName("Sem graça, reapresentar o refresh consumido apaga a sessão inteira")
    void wipesSessionWhenReusedOutsideGrace() {
        final var withoutGrace = store(JwtPropertiesFixture.with(
                JwtPropertiesFixture.SESSION_MAX_SECONDS,
                JwtPropertiesFixture.MAX_SESSIONS_PER_PROFILE,
                0));
        final var profileId = UUID.randomUUID();
        final var access = accessClaims();
        final var refresh = refreshClaims();
        final var session = Session.open(profileId, access.id(), refresh.id());
        withoutGrace.open(session, access, refresh);
        final var nextAccess = accessClaims();
        final var nextRefresh = refreshClaims();
        withoutGrace.rotate(new RotationCommand(refresh.id(), session, nextAccess, nextRefresh));

        final var outcome = withoutGrace.replay(refresh.id());

        assertThat(outcome).isInstanceOf(RotationOutcome.Invalid.class);
        assertThat(profileOfAccess(withoutGrace, nextAccess.id())).isEmpty();
        assertThat(withoutGrace.findActiveRefresh(nextRefresh.id())).isEmpty();
        assertThat(redis.hasKey("session:" + session.id())).isFalse();
    }

    @Test
    @DisplayName("Refresh desconhecido não rotaciona nada")
    void refusesUnknownRefresh() {
        final var profileId = UUID.randomUUID();
        final var access = accessClaims();
        final var refresh = refreshClaims();
        final var session = Session.open(profileId, access.id(), refresh.id());
        store.open(session, access, refresh);

        final var outcome = store.rotate(new RotationCommand(
                UUID.randomUUID(),
                session,
                accessClaims(),
                refreshClaims()));

        assertThat(outcome).isInstanceOf(RotationOutcome.Invalid.class);
        assertThat(store.findActiveRefresh(refresh.id())).contains(session);
    }

    @Test
    @DisplayName("Refresh sem sessão e sem tombstone é inválido")
    void refusesReplayWithoutTombstone() {
        assertThat(store.replay(UUID.randomUUID())).isInstanceOf(RotationOutcome.Invalid.class);
    }

    @Test
    @DisplayName("O access vigente resolve a sessão inteira; sem par, o refresh vem nulo")
    void resolvesWholeSessionFromActiveAccess() {
        final var profileId = UUID.randomUUID();
        final var access = accessClaims();
        final var refresh = refreshClaims();
        final var paired = Session.open(profileId, access.id(), refresh.id());
        store.open(paired, access, refresh);
        final var loneAccess = accessClaims();
        final var lone = Session.open(profileId, loneAccess.id(), null);
        store.open(lone, loneAccess, null);

        final var resolvedPair = store.findActiveAccess(access.id()).orElseThrow();
        final var resolvedLone = store.findActiveAccess(loneAccess.id()).orElseThrow();

        assertThat(resolvedPair.id()).isEqualTo(paired.id());
        assertThat(resolvedPair.profileId()).isEqualTo(profileId);
        assertThat(resolvedPair.accessId()).isEqualTo(access.id());
        assertThat(resolvedPair.refreshId()).isEqualTo(refresh.id());
        assertThat(resolvedLone.id()).isEqualTo(lone.id());
        assertThat(resolvedLone.refreshId()).isNull();
    }

    @Test
    @DisplayName("Lista as sessões do perfil na ordem do login e tira do índice as mortas")
    void listsActiveSessionsInLoginOrder() throws InterruptedException {
        final var profileId = UUID.randomUUID();
        final var first = open(store, profileId);
        Thread.sleep(2);
        final var second = open(store, profileId);
        Thread.sleep(2);
        final var third = open(store, profileId);
        redis.unlink("session:" + second.id());

        assertThat(store.activeSessionIds(profileId)).containsExactly(first.id(), third.id());
        assertThat(redis.opsForZSet().score("profile:" + profileId + ":sessions", second.id().toString())).isNull();
        assertThat(store.activeSessionIds(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("Signout encerra o lote, apaga tombstones e preserva as outras sessões")
    void closesRequestedSessions() {
        final var profileId = UUID.randomUUID();
        final var access = accessClaims();
        final var refresh = refreshClaims();
        final var session = Session.open(profileId, access.id(), refresh.id());
        store.open(session, access, refresh);
        final var nextAccess = accessClaims();
        final var nextRefresh = refreshClaims();
        store.rotate(new RotationCommand(refresh.id(), session, nextAccess, nextRefresh));
        final var kept = open(store, profileId);

        assertThat(store.close(profileId, List.of(session.id(), session.id()))).isTrue();

        assertThat(profileOfAccess(store, nextAccess.id())).isEmpty();
        assertThat(store.findActiveRefresh(nextRefresh.id())).isEmpty();
        assertThat(redis.hasKey("session:" + session.id())).isFalse();
        assertThat(redis.hasKey("tomb:" + refresh.id())).isFalse();
        assertThat(store.activeSessionIds(profileId)).containsExactly(kept.id());
    }

    @Test
    @DisplayName("Id de outro perfil ou inexistente não encerra sessão alguma")
    void refusesForeignOrUnknownSessionsInBatch() {
        final var profileId = UUID.randomUUID();
        final var own = open(store, profileId);
        final var foreign = open(store, UUID.randomUUID());

        assertThat(store.close(profileId, List.of(own.id(), foreign.id()))).isFalse();
        assertThat(store.close(profileId, List.of(own.id(), UUID.randomUUID()))).isFalse();

        assertThat(store.activeSessionIds(profileId)).containsExactly(own.id());
        assertThat(profileOfAccess(store, foreign.accessId())).isPresent();
    }

    @Test
    @DisplayName("Wipe encerra todas as sessões do perfil e não toca nas de outros")
    void wipesEveryProfileSession() {
        final var profileId = UUID.randomUUID();
        final var first = open(store, profileId);
        final var second = open(store, profileId);
        final var other = open(store, UUID.randomUUID());

        store.wipe(profileId);
        store.wipe(UUID.randomUUID());

        assertThat(store.activeSessionIds(profileId)).isEmpty();
        assertThat(profileOfAccess(store, first.accessId())).isEmpty();
        assertThat(profileOfAccess(store, second.accessId())).isEmpty();
        assertThat(redis.hasKey("profile:" + profileId + ":sessions")).isFalse();
        assertThat(profileOfAccess(store, other.accessId())).isPresent();
    }

    @Test
    @DisplayName("Redis fora do ar vira indisponibilidade do store, não token inválido")
    void failsClosedWhenRedisIsDown() {
        final var offline = new LettuceConnectionFactory(new RedisStandaloneConfiguration(HOST, PORT + 20));
        offline.afterPropertiesSet();
        final var offlineTemplate = new StringRedisTemplate(offline);
        offlineTemplate.afterPropertiesSet();
        final var offlineStore = new RedisSessionStore(offlineTemplate, JwtPropertiesFixture.defaults(), CLOCK);

        final var thrown = catchThrowable(() -> offlineStore.findActiveAccess(UUID.randomUUID()));

        assertThat(thrown).isInstanceOf(SessionStoreUnavailableException.class);
        offline.destroy();
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

    private static RedisSessionStore store(final JwtProperties properties) {
        return new RedisSessionStore(redis, properties, CLOCK);
    }

    private static Optional<UUID> profileOfAccess(final RedisSessionStore store, final UUID accessId) {
        return store.findActiveAccess(accessId).map(Session::profileId);
    }

    private static Session open(final RedisSessionStore store, final UUID profileId) {
        final var access = accessClaims();
        final var session = Session.open(profileId, access.id(), null);
        store.open(session, access, null);
        return session;
    }

    private static TokenClaims accessClaims() {
        final var now = Instant.now();
        return TokenClaims.create(TokenUse.ACCESS, now, now.plusSeconds(JwtPropertiesFixture.EXPIRATION_SECONDS));
    }

    private static TokenClaims refreshClaims() {
        final var now = Instant.now();
        return TokenClaims.create(
                TokenUse.REFRESH,
                now,
                now.plusSeconds(JwtPropertiesFixture.REFRESH_EXPIRATION_SECONDS));
    }

}
