package com.sajitar.backend.adapter.out.persistence.profile;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sajitar.backend.domain.model.profile.Profile;

@DisplayName("ProfilePersistenceMapper")
class ProfilePersistenceMapperTest {

    @Test
    @DisplayName("toEntity e toDomain preservam os campos")
    void roundTrip() {
        final var domain = new Profile(
                UUID.fromString("019c0000-a111-7000-8000-111111111111"),
                "Maria Silva",
                "Uma pessoa criativa e dedicada.",
                LocalDate.parse("1988-01-10"),
                "user@example.com",
                "hashed-password");

        final var entity = ProfilePersistenceMapper.toEntity(domain);
        assertThat(entity.getId()).isEqualTo(domain.id());
        assertThat(entity.getName()).isEqualTo(domain.name());
        assertThat(entity.getDescription()).isEqualTo(domain.description());
        assertThat(entity.getBirthday()).isEqualTo(domain.birthday());
        assertThat(entity.getEmail()).isEqualTo(domain.email());
        assertThat(entity.getPassword()).isEqualTo(domain.password());

        final var back = ProfilePersistenceMapper.toDomain(entity);
        assertThat(back.id()).isEqualTo(domain.id());
        assertThat(back.name()).isEqualTo(domain.name());
        assertThat(back.description()).isEqualTo(domain.description());
        assertThat(back.birthday()).isEqualTo(domain.birthday());
        assertThat(back.email()).isEqualTo(domain.email());
        assertThat(back.password()).isEqualTo(domain.password());
    }

}
