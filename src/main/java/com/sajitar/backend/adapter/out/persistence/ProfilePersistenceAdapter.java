package com.sajitar.backend.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sajitar.backend.domain.model.Profile;
import com.sajitar.backend.domain.port.ProfilePageCriteria;
import com.sajitar.backend.domain.port.ProfileRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
class ProfilePersistenceAdapter implements ProfileRepository {

    private final ProfileJpaRepository jpa;

    @Override
    public Profile save(final Profile profile) {
        return ProfilePersistenceMapper.toDomain(jpa.save(ProfilePersistenceMapper.toEntity(profile)));
    }

    @Override
    public Optional<Profile> findById(final UUID id) {
        return jpa.findById(id).map(ProfilePersistenceMapper::toDomain);
    }

    @Override
    public Optional<Profile> findByEmail(final String email) {
        return jpa.findByEmail(email).map(ProfilePersistenceMapper::toDomain);
    }

    @Override
    public void deleteById(final UUID id) {
        jpa.deleteById(id);
    }

    @Override
    public List<Profile> findPage(final ProfilePageCriteria criteria) {
        return findEntities(criteria).stream().map(ProfilePersistenceMapper::toDomain).toList();
    }

    @Override
    public long countAfterCursor(final ProfilePageCriteria criteria) {
        if (criteria.hasNameFilter()) {
            return criteria.reverse()
                    ? jpa.countForFindByNameContainingIgnoreCaseDescendingAfter(
                            criteria.lastSeenName(), criteria.lastSeenId(), criteria.nameContains())
                    : jpa.countForFindByNameContainingIgnoreCaseAscendingAfter(
                            criteria.lastSeenName(), criteria.lastSeenId(), criteria.nameContains());
        }
        return criteria.reverse()
                ? jpa.countForFindAllDescendingAfter(criteria.lastSeenName(), criteria.lastSeenId())
                : jpa.countForFindAllAscendingAfter(criteria.lastSeenName(), criteria.lastSeenId());
    }

    private List<ProfileJpaEntity> findEntities(final ProfilePageCriteria criteria) {
        if (criteria.hasNameFilter()) {
            if (criteria.hasCursor()) {
                return criteria.reverse()
                        ? jpa.findByNameContainingIgnoreCaseDescendingAfter(
                                criteria.limit(),
                                criteria.lastSeenName(),
                                criteria.lastSeenId(),
                                criteria.nameContains())
                        : jpa.findByNameContainingIgnoreCaseAscendingAfter(
                                criteria.limit(),
                                criteria.lastSeenName(),
                                criteria.lastSeenId(),
                                criteria.nameContains());
            }
            return criteria.reverse()
                    ? jpa.findByNameContainingIgnoreCaseDescending(criteria.limit(), criteria.nameContains())
                    : jpa.findByNameContainingIgnoreCaseAscending(criteria.limit(), criteria.nameContains());
        }
        if (criteria.hasCursor()) {
            return criteria.reverse()
                    ? jpa.findAllDescendingAfter(criteria.limit(), criteria.lastSeenName(), criteria.lastSeenId())
                    : jpa.findAllAscendingAfter(criteria.limit(), criteria.lastSeenName(), criteria.lastSeenId());
        }
        return criteria.reverse()
                ? jpa.findAllDescending(criteria.limit())
                : jpa.findAllAscending(criteria.limit());
    }

}
