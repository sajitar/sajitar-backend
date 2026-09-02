package com.sajitar.backend.adapter.out.persistence;

import com.sajitar.backend.domain.model.Profile;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class ProfilePersistenceMapper {

    static Profile toDomain(final ProfileJpaEntity entity) {
        return new Profile(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getBirthday(),
                entity.getEmail(),
                entity.getPassword());
    }

    static ProfileJpaEntity toEntity(final Profile profile) {
        return ProfileJpaEntity.builder()
                .id(profile.id())
                .name(profile.name())
                .description(profile.description())
                .birthday(profile.birthday())
                .email(profile.email())
                .password(profile.password())
                .build();
    }

}
