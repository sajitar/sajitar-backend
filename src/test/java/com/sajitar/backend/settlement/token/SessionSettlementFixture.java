package com.sajitar.backend.settlement.token;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

import lombok.experimental.UtilityClass;

/**
 * Limpeza das chaves de sessão entre testes. Usa SCAN + UNLINK porque o usuário
 * da ACL de {@code /tokens} não tem FLUSHDB nem KEYS.
 */
@UtilityClass
public class SessionSettlementFixture {

    private static final List<String> PATTERNS = List.of("token:*", "tomb:*", "session:*", "profile:*", "attempt:*");

    public static void clear(final StringRedisTemplate redis) {
        PATTERNS.forEach(pattern -> {
            final var keys = new ArrayList<String>();
            try (var cursor = redis.scan(ScanOptions.scanOptions().match(pattern).count(500).build())) {
                cursor.forEachRemaining(keys::add);
            }
            if (!keys.isEmpty()) {
                redis.unlink(keys);
            }
        });
    }

}
