package com.sajitar.backend.adapter.out.persistence.authority;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sajitar.backend.domain.model.authority.Authority;

@DisplayName("AuthorityPersistenceMapper")
class AuthorityPersistenceMapperTest {

    @Test
    @DisplayName("toEntity e toDomain preservam os campos")
    void roundTrip() {
        final var domain = new Authority(
                UUID.fromString("019c2000-a111-7000-8000-111111111111"),
                UUID.fromString("01989bad-6161-7000-0ae9-f440b10578ec"),
                Authority.Type.READER);

        final var entity = AuthorityPersistenceMapper.toEntity(domain);
        assertThat(entity.getId()).isEqualTo(domain.id());
        assertThat(entity.getProfileId()).isEqualTo(domain.profileId());
        assertThat(entity.getType()).isEqualTo(Authority.Type.READER);

        final var back = AuthorityPersistenceMapper.toDomain(entity);
        assertThat(back.id()).isEqualTo(domain.id());
        assertThat(back.profileId()).isEqualTo(domain.profileId());
        assertThat(back.type()).isEqualTo(domain.type());
    }

}
