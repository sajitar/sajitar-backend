package com.sajitar.backend.domain.model.token;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;

public record TokenClaims(UUID id, TokenUse use, Instant issuedAt, Instant expiresAt) {

    private static final TimeBasedEpochGenerator ID_GENERATOR = Generators.timeBasedEpochGenerator();

    /**
     * Instantes ficam em segundos inteiros, a mesma resolução que o JWT carrega:
     * é o que permite reassinar o sucessor na graça a partir das claims gravadas.
     */
    public static TokenClaims create(final TokenUse use, final Instant issuedAt, final Instant expiresAt) {
        return new TokenClaims(
                ID_GENERATOR.generate(),
                use,
                issuedAt.truncatedTo(ChronoUnit.SECONDS),
                expiresAt.truncatedTo(ChronoUnit.SECONDS));
    }

    public long expiresInSeconds(final Instant now) {
        return Math.max(0L, expiresAt.getEpochSecond() - now.getEpochSecond());
    }

}
