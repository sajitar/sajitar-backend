package com.sajitar.backend.adapter.out.persistence.authority;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sajitar.backend.domain.model.authority.Authority;
import com.sajitar.backend.domain.port.authority.AuthorityPageCriteria;
import com.sajitar.backend.domain.port.authority.AuthorityRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
class AuthorityPersistenceAdapter implements AuthorityRepository {

    private final AuthorityJpaRepository jpa;

    @Override
    public Authority save(final Authority authority) {
        return AuthorityPersistenceMapper.toDomain(jpa.save(AuthorityPersistenceMapper.toEntity(authority)));
    }

    @Override
    public Optional<Authority> findById(final UUID id) {
        return jpa.findById(id).map(AuthorityPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Authority> findByProfileIdAndType(final UUID profileId, final Authority.Type type) {
        return jpa.findByProfileIdAndType(profileId, type).map(AuthorityPersistenceMapper::toDomain);
    }

    @Override
    public void deleteById(final UUID id) {
        jpa.deleteById(id);
    }

    @Override
    public List<Authority> findPage(final AuthorityPageCriteria criteria) {
        return findEntities(criteria).stream().map(AuthorityPersistenceMapper::toDomain).toList();
    }

    private List<AuthorityJpaEntity> findEntities(final AuthorityPageCriteria criteria) {
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
    public long countAfterCursor(final AuthorityPageCriteria criteria) {
        if (!criteria.hasCursor()) {
            return 0L;
        }
        final var lastSeenType = (short) criteria.lastSeenType().value();
        return criteria.reverse()
                ? jpa.countByProfileIdAndTypeBefore(criteria.profileId(), lastSeenType)
                : jpa.countByProfileIdAndTypeAfter(criteria.profileId(), lastSeenType);
    }

}
