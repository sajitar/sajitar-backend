package com.sajitar.backend.adapter.out.persistence.profile;

import com.sajitar.backend.domain.model.profile.Profile;

import lombok.experimental.UtilityClass;

@UtilityClass
final class ProfilePersistenceMapper {

    static Profile toDomain(final ProfileJpaEntity entity) {
        return new Profile(
                entity.getId(),
                entity.getType(),
                entity.getName(),
                entity.getDescription(),
                entity.getBirthday(),
                entity.getEmail(),
                entity.getPassword(),
                entity.isTwoFactor());
    }

    static ProfileJpaEntity toEntity(final Profile profile) {
        return ProfileJpaEntity.builder()
                .id(profile.id())
                .type(profile.type())
                .name(profile.name())
                .description(profile.description())
                .birthday(profile.birthday())
                .email(profile.email())
                .password(profile.password())
                .twoFactor(profile.twoFactor())
                .build();
    }

}
