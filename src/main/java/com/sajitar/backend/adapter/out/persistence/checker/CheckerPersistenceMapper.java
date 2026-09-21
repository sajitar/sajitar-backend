package com.sajitar.backend.adapter.out.persistence.checker;

import com.sajitar.backend.domain.model.checker.Checker;

import lombok.experimental.UtilityClass;

@UtilityClass
final class CheckerPersistenceMapper {

    static Checker toDomain(final CheckerJpaEntity entity) {
        return new Checker(
                entity.getId(),
                entity.getProfileId(),
                entity.getType(),
                entity.getCode(),
                entity.getPayload());
    }

    static CheckerJpaEntity toEntity(final Checker checker) {
        return CheckerJpaEntity.builder()
                .id(checker.id())
                .profileId(checker.profileId())
                .type(checker.type())
                .code(checker.code())
                .payload(checker.payload())
                .build();
    }

}
