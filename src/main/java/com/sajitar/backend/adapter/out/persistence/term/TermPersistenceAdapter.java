package com.sajitar.backend.adapter.out.persistence.term;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sajitar.backend.domain.model.term.Term;
import com.sajitar.backend.domain.port.term.TermPageCriteria;
import com.sajitar.backend.domain.port.term.TermRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
class TermPersistenceAdapter implements TermRepository {

    private final TermJpaRepository jpa;

    @Override
    public Term save(final Term term) {
        return TermPersistenceMapper.toDomain(jpa.save(TermPersistenceMapper.toEntity(term)));
    }

    @Override
    public List<Term> findByIdAll(final UUID id) {
        return jpa.findLinesById(id).stream().map(TermPersistenceMapper::toDomain).toList();
    }

    @Override
    public Optional<Term> findByIdAndType(final UUID id, final Term.Type type) {
        return jpa.findById(new TermId(id, type)).map(TermPersistenceMapper::toDomain);
    }

    @Override
    public List<UUID> findPageIds(final TermPageCriteria criteria) {
        if (criteria.hasTypeFilter()) {
            final var type = criteria.type().name();
            if (criteria.reverse()) {
                return criteria.hasCursor()
                        ? jpa.findPageIdsByTypeDescendingAfter(type, criteria.lastSeenId(), criteria.limit())
                        : jpa.findPageIdsByTypeDescending(type, criteria.limit());
            }
            return criteria.hasCursor()
                    ? jpa.findPageIdsByTypeAfter(type, criteria.lastSeenId(), criteria.limit())
                    : jpa.findPageIdsByType(type, criteria.limit());
        }
        if (criteria.reverse()) {
            return criteria.hasCursor()
                    ? jpa.findPageIdsDescendingAfter(criteria.lastSeenId(), criteria.limit())
                    : jpa.findPageIdsDescending(criteria.limit());
        }
        return criteria.hasCursor()
                ? jpa.findPageIdsAfter(criteria.lastSeenId(), criteria.limit())
                : jpa.findPageIds(criteria.limit());
    }

    @Override
    public long countAfterCursor(final TermPageCriteria criteria) {
        if (!criteria.hasCursor()) {
            return 0L;
        }
        if (criteria.hasTypeFilter()) {
            final var type = criteria.type().name();
            return criteria.reverse()
                    ? jpa.countIdsByTypeBefore(type, criteria.lastSeenId())
                    : jpa.countIdsByTypeAfter(type, criteria.lastSeenId());
        }
        return criteria.reverse()
                ? jpa.countIdsBefore(criteria.lastSeenId())
                : jpa.countIdsAfter(criteria.lastSeenId());
    }

    @Override
    public Optional<UUID> findCurrentId() {
        return jpa.findCurrentIds(1).stream().findFirst();
    }

    @Override
    public void deleteById(final UUID id) {
        jpa.deleteAll(jpa.findLinesById(id));
    }

}
