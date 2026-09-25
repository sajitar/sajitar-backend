package com.sajitar.backend.adapter.out.persistence.profile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.model.profile.Profile;
import com.sajitar.backend.domain.port.profile.ProfilePageCriteria;
import com.sajitar.backend.domain.port.profile.ProfileRepository;

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
    public List<UUID> findUnverifiedCreatedBefore(final Instant cutoff) {
        return jpa.findUnverifiedCreatedBefore(
                (short) Checker.Type.VERIFY_EMAIL.value(),
                uuidV7At(cutoff));
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
        final var includeUnverified = criteria.includeUnverified();
        final var verifyEmail = verifyEmailType();
        if (criteria.hasNameFilter()) {
            return criteria.reverse()
                    ? jpa.countForFindByNameContainingIgnoreCaseDescendingAfter(
                            criteria.lastSeenName(),
                            criteria.lastSeenId(),
                            criteria.nameContains(),
                            includeUnverified,
                            verifyEmail)
                    : jpa.countForFindByNameContainingIgnoreCaseAscendingAfter(
                            criteria.lastSeenName(),
                            criteria.lastSeenId(),
                            criteria.nameContains(),
                            includeUnverified,
                            verifyEmail);
        }
        return criteria.reverse()
                ? jpa.countForFindAllDescendingAfter(
                        criteria.lastSeenName(),
                        criteria.lastSeenId(),
                        includeUnverified,
                        verifyEmail)
                : jpa.countForFindAllAscendingAfter(
                        criteria.lastSeenName(),
                        criteria.lastSeenId(),
                        includeUnverified,
                        verifyEmail);
    }

    private List<ProfileJpaEntity> findEntities(final ProfilePageCriteria criteria) {
        final var includeUnverified = criteria.includeUnverified();
        final var verifyEmail = verifyEmailType();
        if (criteria.hasNameFilter()) {
            if (criteria.hasCursor()) {
                return criteria.reverse()
                        ? jpa.findByNameContainingIgnoreCaseDescendingAfter(
                                criteria.limit(),
                                criteria.lastSeenName(),
                                criteria.lastSeenId(),
                                criteria.nameContains(),
                                includeUnverified,
                                verifyEmail)
                        : jpa.findByNameContainingIgnoreCaseAscendingAfter(
                                criteria.limit(),
                                criteria.lastSeenName(),
                                criteria.lastSeenId(),
                                criteria.nameContains(),
                                includeUnverified,
                                verifyEmail);
            }
            return criteria.reverse()
                    ? jpa.findByNameContainingIgnoreCaseDescending(
                            criteria.limit(),
                            criteria.nameContains(),
                            includeUnverified,
                            verifyEmail)
                    : jpa.findByNameContainingIgnoreCaseAscending(
                            criteria.limit(),
                            criteria.nameContains(),
                            includeUnverified,
                            verifyEmail);
        }
        if (criteria.hasCursor()) {
            return criteria.reverse()
                    ? jpa.findAllDescendingAfter(
                            criteria.limit(),
                            criteria.lastSeenName(),
                            criteria.lastSeenId(),
                            includeUnverified,
                            verifyEmail)
                    : jpa.findAllAscendingAfter(
                            criteria.limit(),
                            criteria.lastSeenName(),
                            criteria.lastSeenId(),
                            includeUnverified,
                            verifyEmail);
        }
        return criteria.reverse()
                ? jpa.findAllDescending(criteria.limit(), includeUnverified, verifyEmail)
                : jpa.findAllAscending(criteria.limit(), includeUnverified, verifyEmail);
    }

    private static short verifyEmailType() {
        return (short) Checker.Type.VERIFY_EMAIL.value();
    }

    static UUID uuidV7At(final Instant instant) {
        return Checker.uuidV7At(instant);
    }

}
