package com.sajitar.backend.adapter.out.persistence.checker;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sajitar.backend.domain.model.checker.Checker;

@DisplayName("CheckerPersistenceMapper")
class CheckerPersistenceMapperTest {

    @Test
    @DisplayName("toEntity e toDomain preservam os campos")
    void roundTrip() {
        final var domain = new Checker(
                UUID.fromString("019c1000-a111-7000-8000-111111111111"),
                UUID.fromString("01989bad-6161-7000-0ae9-f440b10578ec"),
                Checker.Type.CHANGE_PASSWORD,
                "345678",
                "secret",
                4,
                1,
                Instant.parse("2001-04-24T21:00:00Z"));

        final var entity = CheckerPersistenceMapper.toEntity(domain);
        assertThat(entity.getId()).isEqualTo(domain.id());
        assertThat(entity.getProfileId()).isEqualTo(domain.profileId());
        assertThat(entity.getType()).isEqualTo(Checker.Type.CHANGE_PASSWORD);
        assertThat(entity.getCode()).isEqualTo("345678");
        assertThat(entity.getPayload()).isEqualTo("secret");
        assertThat(entity.getAttempts()).isEqualTo((short) 4);
        assertThat(entity.getReplaces()).isEqualTo((short) 1);
        assertThat(entity.getUpdatedAt()).isEqualTo(domain.updatedAt());

        final var back = CheckerPersistenceMapper.toDomain(entity);
        assertThat(back.id()).isEqualTo(domain.id());
        assertThat(back.profileId()).isEqualTo(domain.profileId());
        assertThat(back.type()).isEqualTo(domain.type());
        assertThat(back.code()).isEqualTo(domain.code());
        assertThat(back.payload()).isEqualTo(domain.payload());
        assertThat(back.attempts()).isEqualTo(domain.attempts());
        assertThat(back.replaces()).isEqualTo(domain.replaces());
        assertThat(back.updatedAt()).isEqualTo(domain.updatedAt());
    }

}
