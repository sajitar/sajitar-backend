package com.sajitar.backend.adapter.out.persistence.term;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sajitar.backend.domain.model.term.Term;
import com.sajitar.backend.domain.model.term.TermDetail;
import com.sajitar.backend.domain.port.term.TermPageCriteria;
import com.sajitar.backend.domain.port.term.TermRepository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
class TermPersistenceAdapter implements TermRepository {

    private final TermJpaRepository terms;

    private final TermDetailJpaRepository details;

    private final EntityManager entityManager;

    @Override
    public TermDetail save(final Term term, final TermDetail detail) {
        terms.saveAndFlush(TermPersistenceMapper.toTermEntity(term));
        return TermPersistenceMapper.toDomain(details.save(TermPersistenceMapper.toDetailEntity(detail)));
    }

    @Override
    public List<TermDetail> findByIdAll(final UUID id) {
        return details.findLinesById(id).stream().map(TermPersistenceMapper::toDomain).toList();
    }

    @Override
    public Optional<TermDetail> findByIdAndType(final UUID id, final TermDetail.Type type) {
        return details.findById(new TermId(id, type)).map(TermPersistenceMapper::toDomain);
    }

    @Override
    public List<UUID> findPageIds(final TermPageCriteria criteria) {
        if (criteria.hasTypeFilter()) {
            final var type = criteria.type().name();
            if (criteria.reverse()) {
                return criteria.hasCursor()
                        ? details.findPageIdsByTypeDescendingAfter(type, criteria.lastSeenId(), criteria.limit())
                        : details.findPageIdsByTypeDescending(type, criteria.limit());
            }
            return criteria.hasCursor()
                    ? details.findPageIdsByTypeAfter(type, criteria.lastSeenId(), criteria.limit())
                    : details.findPageIdsByType(type, criteria.limit());
        }
        if (criteria.reverse()) {
            return criteria.hasCursor()
                    ? details.findPageIdsDescendingAfter(criteria.lastSeenId(), criteria.limit())
                    : details.findPageIdsDescending(criteria.limit());
        }
        return criteria.hasCursor()
                ? details.findPageIdsAfter(criteria.lastSeenId(), criteria.limit())
                : details.findPageIds(criteria.limit());
    }

    @Override
    public long countAfterCursor(final TermPageCriteria criteria) {
        if (!criteria.hasCursor()) {
            return 0L;
        }
        if (criteria.hasTypeFilter()) {
            final var type = criteria.type().name();
            return criteria.reverse()
                    ? details.countIdsByTypeBefore(type, criteria.lastSeenId())
                    : details.countIdsByTypeAfter(type, criteria.lastSeenId());
        }
        return criteria.reverse()
                ? details.countIdsBefore(criteria.lastSeenId())
                : details.countIdsAfter(criteria.lastSeenId());
    }

    @Override
    public Optional<UUID> findCurrentId() {
        return details.findCurrentIds(1).stream().findFirst();
    }

    @Override
    public void deleteById(final UUID id) {
        details.findLinesById(id).forEach(line -> {
            if (entityManager.contains(line)) {
                entityManager.detach(line);
            }
        });
        terms.deleteById(id);
    }

}
