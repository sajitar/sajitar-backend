package com.sajitar.backend.adapter.out.persistence.note;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sajitar.backend.domain.model.note.Note;

@DisplayName("NotePersistenceMapper")
class NotePersistenceMapperTest {

    @Test
    @DisplayName("toEntity e toDomain preservam os campos")
    void roundTrip() {
        final var domain = new Note(
                UUID.fromString("019c3000-a111-7000-8000-111111111111"),
                UUID.fromString("01989bad-6161-7000-0ae9-f440b10578ec"),
                Note.Type.PRIVATE,
                "Alice privated");

        final var entity = NotePersistenceMapper.toEntity(domain);
        assertThat(entity.getId()).isEqualTo(domain.id());
        assertThat(entity.getProfileId()).isEqualTo(domain.profileId());
        assertThat(entity.getType()).isEqualTo(Note.Type.PRIVATE);
        assertThat(entity.getContent()).isEqualTo("Alice privated");

        final var back = NotePersistenceMapper.toDomain(entity);
        assertThat(back.id()).isEqualTo(domain.id());
        assertThat(back.profileId()).isEqualTo(domain.profileId());
        assertThat(back.type()).isEqualTo(domain.type());
        assertThat(back.content()).isEqualTo(domain.content());
    }

}
