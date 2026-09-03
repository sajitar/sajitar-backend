package com.sajitar.backend.adapter.out.persistence.checker;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.port.checker.CheckerPageCriteria;
import com.sajitar.backend.domain.port.checker.CheckerRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
class CheckerPersistenceAdapter implements CheckerRepository {

    private final CheckerJpaRepository jpa;

    @Override
    public Checker save(final Checker checker) {
        return CheckerPersistenceMapper.toDomain(jpa.save(CheckerPersistenceMapper.toEntity(checker)));
    }

    @Override
    public Optional<Checker> findById(final UUID id) {
        return jpa.findById(id).map(CheckerPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Checker> findByProfileIdAndType(final UUID profileId, final Checker.Type type) {
        return jpa.findByProfileIdAndType(profileId, type).map(CheckerPersistenceMapper::toDomain);
    }

    @Override
    public void deleteById(final UUID id) {
        jpa.deleteById(id);
    }

    @Override
    public List<Checker> findPage(final CheckerPageCriteria criteria) {
        return findEntities(criteria).stream().map(CheckerPersistenceMapper::toDomain).toList();
    }

    private List<CheckerJpaEntity> findEntities(final CheckerPageCriteria criteria) {
        if (criteria.reverse()) {
            return criteria.hasCursor()
                    ? jpa.findPageByProfileIdDescendingAfter(
                            criteria.profileId(),
                            (short) criteria.lastSeenType().value(),
                            criteria.limit())
                    : jpa.findPageByProfileIdDescending(criteria.profileId(), criteria.limit());
        }
        return criteria.hasCursor()
                ? jpa.findPageByProfileIdAfter(
                        criteria.profileId(),
                        (short) criteria.lastSeenType().value(),
                        criteria.limit())
                : jpa.findPageByProfileId(criteria.profileId(), criteria.limit());
    }

    @Override
    public long countAfterCursor(final CheckerPageCriteria criteria) {
        if (!criteria.hasCursor()) {
            return 0L;
        }
        final var lastSeenType = (short) criteria.lastSeenType().value();
        return criteria.reverse()
                ? jpa.countByProfileIdAndTypeBefore(criteria.profileId(), lastSeenType)
                : jpa.countByProfileIdAndTypeAfter(criteria.profileId(), lastSeenType);
    }

}
