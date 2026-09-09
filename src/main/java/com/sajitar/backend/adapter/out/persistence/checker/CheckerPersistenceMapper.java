package com.sajitar.backend.adapter.out.persistence.checker;

import com.sajitar.backend.domain.model.checker.Checker;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class CheckerPersistenceMapper {

    static Checker toDomain(final CheckerJpaEntity entity) {
        return new Checker(
                entity.getId(),
                entity.getProfileId(),
                entity.getType(),
                entity.getCode(),
                entity.getPayload(),
                entity.getAttempts(),
                entity.getReplaces(),
                entity.getUpdatedAt());
    }

    static CheckerJpaEntity toEntity(final Checker checker) {
        return CheckerJpaEntity.builder()
                .id(checker.id())
                .profileId(checker.profileId())
                .type(checker.type())
                .code(checker.code())
                .payload(checker.payload())
                .attempts((short) checker.attempts())
                .replaces((short) checker.replaces())
                .updatedAt(checker.updatedAt())
                .build();
    }

}
