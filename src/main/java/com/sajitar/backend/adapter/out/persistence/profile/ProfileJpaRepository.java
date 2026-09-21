package com.sajitar.backend.adapter.out.persistence.profile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sajitar.backend.domain.model.checker.Checker;

public interface ProfileJpaRepository extends JpaRepository<ProfileJpaEntity, UUID> {

    Optional<ProfileJpaEntity> findByEmail(String email);

    @Query(nativeQuery = true, value = """
            select p.id from profile p
            inner join checker c on c.profile_id = p.id and c.type = :verifyEmail
            where p.id < :cutoffId
            """)
    List<UUID> findUnverifiedCreatedBefore(
            final @Param("verifyEmail") short verifyEmail,
            final @Param("cutoffId") UUID cutoffId);

    @Query(nativeQuery = true, value = """
            select * from profile
            where (:includeUnverified or not exists (
                select 1 from checker c
                where c.profile_id = profile.id and c.type = :verifyEmail
            ))
            order by (name_purified, id) asc
            limit :limit
            """)
    List<ProfileJpaEntity> findAllAscending(
            final @Param("limit") int limit,
            final @Param("includeUnverified") boolean includeUnverified,
            final @Param("verifyEmail") short verifyEmail);

    @Query(nativeQuery = true, value = """
            select * from profile
            where (name_purified, id) > (purify(:lastSeenName), :lastSeenId)
            and (:includeUnverified or not exists (
                select 1 from checker c
                where c.profile_id = profile.id and c.type = :verifyEmail
            ))
            order by (name_purified, id) asc
            limit :limit
            """)
    List<ProfileJpaEntity> findAllAscendingAfter(
            final @Param("limit") int limit,
            final @Param("lastSeenName") String lastSeenName,
            final @Param("lastSeenId") UUID lastSeenId,
            final @Param("includeUnverified") boolean includeUnverified,
            final @Param("verifyEmail") short verifyEmail);

    @Query(nativeQuery = true, value = """
            select * from profile
            where (:includeUnverified or not exists (
                select 1 from checker c
                where c.profile_id = profile.id and c.type = :verifyEmail
            ))
            order by (name_purified, id) desc
            limit :limit
            """)
    List<ProfileJpaEntity> findAllDescending(
            final @Param("limit") int limit,
            final @Param("includeUnverified") boolean includeUnverified,
            final @Param("verifyEmail") short verifyEmail);

    @Query(nativeQuery = true, value = """
            select * from profile
            where (name_purified, id) < (purify(:lastSeenName), :lastSeenId)
            and (:includeUnverified or not exists (
                select 1 from checker c
                where c.profile_id = profile.id and c.type = :verifyEmail
            ))
            order by (name_purified, id) desc
            limit :limit
            """)
    List<ProfileJpaEntity> findAllDescendingAfter(
            final @Param("limit") int limit,
            final @Param("lastSeenName") String lastSeenName,
            final @Param("lastSeenId") UUID lastSeenId,
            final @Param("includeUnverified") boolean includeUnverified,
            final @Param("verifyEmail") short verifyEmail);

    @Query(nativeQuery = true, value = """
            select * from profile
            where
                name_purified like '%' || purify(:name) || '%'
                and (:includeUnverified or not exists (
                    select 1 from checker c
                    where c.profile_id = profile.id and c.type = :verifyEmail
                ))
            order by (name_purified, id) asc
            limit :limit
            """)
    List<ProfileJpaEntity> findByNameContainingIgnoreCaseAscending(
            final @Param("limit") int limit,
            final @Param("name") String name,
            final @Param("includeUnverified") boolean includeUnverified,
            final @Param("verifyEmail") short verifyEmail);

    @Query(nativeQuery = true, value = """
            select * from profile
            where
                (name_purified, id) > (purify(:lastSeenName), :lastSeenId)
                and
                name_purified like '%' || purify(:name) || '%'
                and (:includeUnverified or not exists (
                    select 1 from checker c
                    where c.profile_id = profile.id and c.type = :verifyEmail
                ))
            order by (name_purified, id) asc
            limit :limit
            """)
    List<ProfileJpaEntity> findByNameContainingIgnoreCaseAscendingAfter(
            final @Param("limit") int limit,
            final @Param("lastSeenName") String lastSeenName,
            final @Param("lastSeenId") UUID lastSeenId,
            final @Param("name") String name,
            final @Param("includeUnverified") boolean includeUnverified,
            final @Param("verifyEmail") short verifyEmail);

    @Query(nativeQuery = true, value = """
            select * from profile
            where
                name_purified like '%' || purify(:name) || '%'
                and (:includeUnverified or not exists (
                    select 1 from checker c
                    where c.profile_id = profile.id and c.type = :verifyEmail
                ))
            order by (name_purified, id) desc
            limit :limit
            """)
    List<ProfileJpaEntity> findByNameContainingIgnoreCaseDescending(
            final @Param("limit") int limit,
            final @Param("name") String name,
            final @Param("includeUnverified") boolean includeUnverified,
            final @Param("verifyEmail") short verifyEmail);

    @Query(nativeQuery = true, value = """
            select * from profile
            where
                (name_purified, id) < (purify(:lastSeenName), :lastSeenId)
                and
                name_purified like '%' || purify(:name) || '%'
                and (:includeUnverified or not exists (
                    select 1 from checker c
                    where c.profile_id = profile.id and c.type = :verifyEmail
                ))
            order by (name_purified, id) desc
            limit :limit
            """)
    List<ProfileJpaEntity> findByNameContainingIgnoreCaseDescendingAfter(
            final @Param("limit") int limit,
            final @Param("lastSeenName") String lastSeenName,
            final @Param("lastSeenId") UUID lastSeenId,
            final @Param("name") String name,
            final @Param("includeUnverified") boolean includeUnverified,
            final @Param("verifyEmail") short verifyEmail);

    @Query(nativeQuery = true, value = "select count(*) from profile")
    long countForFindAll();

    @Query(nativeQuery = true, value = """
            select count(*) from profile
            where (name_purified, id) > (purify(:lastSeenName), :lastSeenId)
            and (:includeUnverified or not exists (
                select 1 from checker c
                where c.profile_id = profile.id and c.type = :verifyEmail
            ))
            """)
    long countForFindAllAscendingAfter(
            final @Param("lastSeenName") String lastSeenName,
            final @Param("lastSeenId") UUID lastSeenId,
            final @Param("includeUnverified") boolean includeUnverified,
            final @Param("verifyEmail") short verifyEmail);

    @Query(nativeQuery = true, value = """
            select count(*) from profile
            where (name_purified, id) < (purify(:lastSeenName), :lastSeenId)
            and (:includeUnverified or not exists (
                select 1 from checker c
                where c.profile_id = profile.id and c.type = :verifyEmail
            ))
            """)
    long countForFindAllDescendingAfter(
            final @Param("lastSeenName") String lastSeenName,
            final @Param("lastSeenId") UUID lastSeenId,
            final @Param("includeUnverified") boolean includeUnverified,
            final @Param("verifyEmail") short verifyEmail);

    @Query(nativeQuery = true, value = """
            select count(*) from profile
            where
                name_purified like '%' || purify(:name) || '%'
                and (:includeUnverified or not exists (
                    select 1 from checker c
                    where c.profile_id = profile.id and c.type = :verifyEmail
                ))
            """)
    long countForFindByNameContainingIgnoreCase(
            final @Param("name") String name,
            final @Param("includeUnverified") boolean includeUnverified,
            final @Param("verifyEmail") short verifyEmail);

    @Query(nativeQuery = true, value = """
            select count(*) from profile
            where
                (name_purified, id) > (purify(:lastSeenName), :lastSeenId)
                and
                name_purified like '%' || purify(:name) || '%'
                and (:includeUnverified or not exists (
                    select 1 from checker c
                    where c.profile_id = profile.id and c.type = :verifyEmail
                ))
            """)
    long countForFindByNameContainingIgnoreCaseAscendingAfter(
            final @Param("lastSeenName") String lastSeenName,
            final @Param("lastSeenId") UUID lastSeenId,
            final @Param("name") String name,
            final @Param("includeUnverified") boolean includeUnverified,
            final @Param("verifyEmail") short verifyEmail);

    @Query(nativeQuery = true, value = """
            select count(*) from profile
            where
                (name_purified, id) < (purify(:lastSeenName), :lastSeenId)
                and
                name_purified like '%' || purify(:name) || '%'
                and (:includeUnverified or not exists (
                    select 1 from checker c
                    where c.profile_id = profile.id and c.type = :verifyEmail
                ))
            """)
    long countForFindByNameContainingIgnoreCaseDescendingAfter(
            final @Param("lastSeenName") String lastSeenName,
            final @Param("lastSeenId") UUID lastSeenId,
            final @Param("name") String name,
            final @Param("includeUnverified") boolean includeUnverified,
            final @Param("verifyEmail") short verifyEmail);

    default List<ProfileJpaEntity> findAllAscending(final int limit) {
        return findAllAscending(limit, true, verifyEmailType());
    }

    default List<ProfileJpaEntity> findAllAscendingAfter(
            final int limit,
            final String lastSeenName,
            final UUID lastSeenId) {
        return findAllAscendingAfter(limit, lastSeenName, lastSeenId, true, verifyEmailType());
    }

    default List<ProfileJpaEntity> findAllDescending(final int limit) {
        return findAllDescending(limit, true, verifyEmailType());
    }

    default List<ProfileJpaEntity> findAllDescendingAfter(
            final int limit,
            final String lastSeenName,
            final UUID lastSeenId) {
        return findAllDescendingAfter(limit, lastSeenName, lastSeenId, true, verifyEmailType());
    }

    default List<ProfileJpaEntity> findByNameContainingIgnoreCaseAscending(final int limit, final String name) {
        return findByNameContainingIgnoreCaseAscending(limit, name, true, verifyEmailType());
    }

    default List<ProfileJpaEntity> findByNameContainingIgnoreCaseAscendingAfter(
            final int limit,
            final String lastSeenName,
            final UUID lastSeenId,
            final String name) {
        return findByNameContainingIgnoreCaseAscendingAfter(
                limit,
                lastSeenName,
                lastSeenId,
                name,
                true,
                verifyEmailType());
    }

    default List<ProfileJpaEntity> findByNameContainingIgnoreCaseDescending(final int limit, final String name) {
        return findByNameContainingIgnoreCaseDescending(limit, name, true, verifyEmailType());
    }

    default List<ProfileJpaEntity> findByNameContainingIgnoreCaseDescendingAfter(
            final int limit,
            final String lastSeenName,
            final UUID lastSeenId,
            final String name) {
        return findByNameContainingIgnoreCaseDescendingAfter(
                limit,
                lastSeenName,
                lastSeenId,
                name,
                true,
                verifyEmailType());
    }

    default long countForFindAllAscendingAfter(final String lastSeenName, final UUID lastSeenId) {
        return countForFindAllAscendingAfter(lastSeenName, lastSeenId, true, verifyEmailType());
    }

    default long countForFindAllDescendingAfter(final String lastSeenName, final UUID lastSeenId) {
        return countForFindAllDescendingAfter(lastSeenName, lastSeenId, true, verifyEmailType());
    }

    default long countForFindByNameContainingIgnoreCaseAscendingAfter(
            final String lastSeenName,
            final UUID lastSeenId,
            final String name) {
        return countForFindByNameContainingIgnoreCaseAscendingAfter(
                lastSeenName,
                lastSeenId,
                name,
                true,
                verifyEmailType());
    }

    default long countForFindByNameContainingIgnoreCaseDescendingAfter(
            final String lastSeenName,
            final UUID lastSeenId,
            final String name) {
        return countForFindByNameContainingIgnoreCaseDescendingAfter(
                lastSeenName,
                lastSeenId,
                name,
                true,
                verifyEmailType());
    }

    private static short verifyEmailType() {
        return (short) Checker.Type.VERIFY_EMAIL.value();
    }

}
