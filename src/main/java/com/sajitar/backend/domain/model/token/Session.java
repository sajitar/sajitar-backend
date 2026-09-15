package com.sajitar.backend.domain.model.token;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;

public record Session(UUID id, UUID profileId, UUID accessId, UUID refreshId) {

    private static final TimeBasedEpochGenerator ID_GENERATOR = Generators.timeBasedEpochGenerator();

    public static Session open(final UUID profileId, final UUID accessId, final UUID refreshId) {
        return new Session(ID_GENERATOR.generate(), profileId, accessId, refreshId);
    }

    /**
     * Instante do login, lido dos 48 bits de tempo do UUIDv7 da sessão. O id nasce
     * no signin e não muda na rotação, então ele também é o relógio do teto
     * absoluto.
     */
    public Instant bornAt() {
        return Instant.ofEpochMilli(id.getMostSignificantBits() >>> 16);
    }

    public Session withRefresh(final UUID refreshId) {
        return new Session(id, profileId, accessId, refreshId);
    }

    @Override
    public boolean equals(final Object object) {
        return object instanceof final Session other && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

}
