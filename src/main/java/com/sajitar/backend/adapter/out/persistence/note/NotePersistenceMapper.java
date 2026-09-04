package com.sajitar.backend.adapter.out.persistence.note;

import com.sajitar.backend.domain.model.note.Note;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class NotePersistenceMapper {

    static Note toDomain(final NoteJpaEntity entity) {
        return new Note(entity.getId(), entity.getProfileId(), entity.getType(), entity.getContent());
    }

    static NoteJpaEntity toEntity(final Note note) {
        return NoteJpaEntity.builder()
                .id(note.id())
                .profileId(note.profileId())
                .type(note.type())
                .content(note.content())
                .build();
    }

}
