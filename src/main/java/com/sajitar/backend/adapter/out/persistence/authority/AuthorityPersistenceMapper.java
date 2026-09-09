package com.sajitar.backend.adapter.out.persistence.authority;

import com.sajitar.backend.domain.model.authority.Authority;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class AuthorityPersistenceMapper {

    static Authority toDomain(final AuthorityJpaEntity entity) {
        return new Authority(entity.getId(), entity.getProfileId(), entity.getType());
    }

    static AuthorityJpaEntity toEntity(final Authority authority) {
        return AuthorityJpaEntity.builder()
                .id(authority.id())
                .profileId(authority.profileId())
                .type(authority.type())
                .build();
    }

}
