package com.sajitar.backend.adapter.out.persistence.profile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.model.profile.Profile;

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
            and (:includeReaders or type <> :reader) and id <> :viewerId and (:unfilteredType or type = :type) and (:unfilteredVerified or (exists (select 1 from checker c where c.profile_id = profile.id and c.type = :verifyEmail) = (not :verified)))
            order by (name_purified, id) asc
            limit :limit
            """)
    List<ProfileJpaEntity> findAllAscending(
            final @Param("limit") int limit,
            final @Param("includeUnverified") boolean includeUnverified,
            final @Param("verifyEmail") short verifyEmail,
            final @Param("includeReaders") boolean includeReaders,
            final @Param("reader") short reader,
            final @Param("viewerId") UUID viewerId,
            final @Param("unfilteredType") boolean unfilteredType,
            final @Param("type") short type,
            final @Param("unfilteredVerified") boolean unfilteredVerified,
            final @Param("verified") boolean verified);

    @Query(nativeQuery = true, value = """
            select * from profile
            where (name_purified, id) > (purify(:lastSeenName), :lastSeenId)
            and (:includeUnverified or not exists (
                select 1 from checker c
                where c.profile_id = profile.id and c.type = :verifyEmail
            ))
            and (:includeReaders or type <> :reader) and id <> :viewerId and (:unfilteredType or type = :type) and (:unfilteredVerified or (exists (select 1 from checker c where c.profile_id = profile.id and c.type = :verifyEmail) = (not :verified)))
            order by (name_purified, id) asc
            limit :limit
            """)
    List<ProfileJpaEntity> findAllAscendingAfter(
            final @Param("limit") int limit,
            final @Param("lastSeenName") String lastSeenName,
            final @Param("lastSeenId") UUID lastSeenId,
            final @Param("includeUnverified") boolean includeUnverified,
            final @Param("verifyEmail") short verifyEmail,
            final @Param("includeReaders") boolean includeReaders,
            final @Param("reader") short reader,
            final @Param("viewerId") UUID viewerId,
            final @Param("unfilteredType") boolean unfilteredType,
            final @Param("type") short type,
            final @Param("unfilteredVerified") boolean unfilteredVerified,
            final @Param("verified") boolean verified);

    @Query(nativeQuery = true, value = """
            select * from profile
            where (:includeUnverified or not exists (
                select 1 from checker c
                where c.profile_id = profile.id and c.type = :verifyEmail
            ))
            and (:includeReaders or type <> :reader) and id <> :viewerId and (:unfilteredType or type = :type) and (:unfilteredVerified or (exists (select 1 from checker c where c.profile_id = profile.id and c.type = :verifyEmail) = (not :verified)))
            order by (name_purified, id) desc
            limit :limit
            """)
    List<ProfileJpaEntity> findAllDescending(
            final @Param("limit") int limit,
            final @Param("includeUnverified") boolean includeUnverified,
            final @Param("verifyEmail") short verifyEmail,
            final @Param("includeReaders") boolean includeReaders,
            final @Param("reader") short reader,
            final @Param("viewerId") UUID viewerId,
            final @Param("unfilteredType") boolean unfilteredType,
            final @Param("type") short type,
            final @Param("unfilteredVerified") boolean unfilteredVerified,
            final @Param("verified") boolean verified);

    @Query(nativeQuery = true, value = """
            select * from profile
            where (name_purified, id) < (purify(:lastSeenName), :lastSeenId)
            and (:includeUnverified or not exists (
                select 1 from checker c
                where c.profile_id = profile.id and c.type = :verifyEmail
            ))
            and (:includeReaders or type <> :reader) and id <> :viewerId and (:unfilteredType or type = :type) and (:unfilteredVerified or (exists (select 1 from checker c where c.profile_id = profile.id and c.type = :verifyEmail) = (not :verified)))
            order by (name_purified, id) desc
            limit :limit
            """)
    List<ProfileJpaEntity> findAllDescendingAfter(
            final @Param("limit") int limit,
            final @Param("lastSeenName") String lastSeenName,
            final @Param("lastSeenId") UUID lastSeenId,
            final @Param("includeUnverified") boolean includeUnverified,
            final @Param("verifyEmail") short verifyEmail,
            final @Param("includeReaders") boolean includeReaders,
            final @Param("reader") short reader,
            final @Param("viewerId") UUID viewerId,
            final @Param("unfilteredType") boolean unfilteredType,
            final @Param("type") short type,
            final @Param("unfilteredVerified") boolean unfilteredVerified,
            final @Param("verified") boolean verified);

    @Query(nativeQuery = true, value = """
            select * from profile
            where
                name_purified like '%' || purify(:name) || '%'
                and (:includeUnverified or not exists (
                    select 1 from checker c
                    where c.profile_id = profile.id and c.type = :verifyEmail
                ))
                and (:includeReaders or type <> :reader) and id <> :viewerId and (:unfilteredType or type = :type) and (:unfilteredVerified or (exists (select 1 from checker c where c.profile_id = profile.id and c.type = :verifyEmail) = (not :verified)))
            order by (name_purified, id) asc
            limit :limit
            """)
    List<ProfileJpaEntity> findByNameContainingIgnoreCaseAscending(
            final @Param("limit") int limit,
            final @Param("name") String name,
            final @Param("includeUnverified") boolean includeUnverified,
            final @Param("verifyEmail") short verifyEmail,
            final @Param("includeReaders") boolean includeReaders,
            final @Param("reader") short reader,
            final @Param("viewerId") UUID viewerId,
            final @Param("unfilteredType") boolean unfilteredType,
            final @Param("type") short type,
            final @Param("unfilteredVerified") boolean unfilteredVerified,
            final @Param("verified") boolean verified);

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
                and (:includeReaders or type <> :reader) and id <> :viewerId and (:unfilteredType or type = :type) and (:unfilteredVerified or (exists (select 1 from checker c where c.profile_id = profile.id and c.type = :verifyEmail) = (not :verified)))
            order by (name_purified, id) asc
            limit :limit
            """)
    List<ProfileJpaEntity> findByNameContainingIgnoreCaseAscendingAfter(
            final @Param("limit") int limit,
            final @Param("lastSeenName") String lastSeenName,
            final @Param("lastSeenId") UUID lastSeenId,
            final @Param("name") String name,
            final @Param("includeUnverified") boolean includeUnverified,
            final @Param("verifyEmail") short verifyEmail,
            final @Param("includeReaders") boolean includeReaders,
            final @Param("reader") short reader,
            final @Param("viewerId") UUID viewerId,
            final @Param("unfilteredType") boolean unfilteredType,
            final @Param("type") short type,
            final @Param("unfilteredVerified") boolean unfilteredVerified,
            final @Param("verified") boolean verified);

    @Query(nativeQuery = true, value = """
            select * from profile
            where
                name_purified like '%' || purify(:name) || '%'
                and (:includeUnverified or not exists (
                    select 1 from checker c
                    where c.profile_id = profile.id and c.type = :verifyEmail
                ))
                and (:includeReaders or type <> :reader) and id <> :viewerId and (:unfilteredType or type = :type) and (:unfilteredVerified or (exists (select 1 from checker c where c.profile_id = profile.id and c.type = :verifyEmail) = (not :verified)))
            order by (name_purified, id) desc
            limit :limit
            """)
    List<ProfileJpaEntity> findByNameContainingIgnoreCaseDescending(
            final @Param("limit") int limit,
            final @Param("name") String name,
            final @Param("includeUnverified") boolean includeUnverified,
            final @Param("verifyEmail") short verifyEmail,
            final @Param("includeReaders") boolean includeReaders,
            final @Param("reader") short reader,
            final @Param("viewerId") UUID viewerId,
            final @Param("unfilteredType") boolean unfilteredType,
            final @Param("type") short type,
            final @Param("unfilteredVerified") boolean unfilteredVerified,
            final @Param("verified") boolean verified);

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
                and (:includeReaders or type <> :reader) and id <> :viewerId and (:unfilteredType or type = :type) and (:unfilteredVerified or (exists (select 1 from checker c where c.profile_id = profile.id and c.type = :verifyEmail) = (not :verified)))
            order by (name_purified, id) desc
            limit :limit
            """)
    List<ProfileJpaEntity> findByNameContainingIgnoreCaseDescendingAfter(
            final @Param("limit") int limit,
            final @Param("lastSeenName") String lastSeenName,
            final @Param("lastSeenId") UUID lastSeenId,
            final @Param("name") String name,
            final @Param("includeUnverified") boolean includeUnverified,
            final @Param("verifyEmail") short verifyEmail,
            final @Param("includeReaders") boolean includeReaders,
            final @Param("reader") short reader,
            final @Param("viewerId") UUID viewerId,
            final @Param("unfilteredType") boolean unfilteredType,
            final @Param("type") short type,
            final @Param("unfilteredVerified") boolean unfilteredVerified,
            final @Param("verified") boolean verified);

    @Query(nativeQuery = true, value = "select count(*) from profile")
    long countForFindAll();

    @Query(nativeQuery = true, value = """
            select count(*) from profile
            where (name_purified, id) > (purify(:lastSeenName), :lastSeenId)
            and (:includeUnverified or not exists (
                select 1 from checker c
                where c.profile_id = profile.id and c.type = :verifyEmail
            ))
            and (:includeReaders or type <> :reader) and id <> :viewerId and (:unfilteredType or type = :type) and (:unfilteredVerified or (exists (select 1 from checker c where c.profile_id = profile.id and c.type = :verifyEmail) = (not :verified)))
            """)
    long countForFindAllAscendingAfter(
            final @Param("lastSeenName") String lastSeenName,
            final @Param("lastSeenId") UUID lastSeenId,
            final @Param("includeUnverified") boolean includeUnverified,
            final @Param("verifyEmail") short verifyEmail,
            final @Param("includeReaders") boolean includeReaders,
            final @Param("reader") short reader,
            final @Param("viewerId") UUID viewerId,
            final @Param("unfilteredType") boolean unfilteredType,
            final @Param("type") short type,
            final @Param("unfilteredVerified") boolean unfilteredVerified,
            final @Param("verified") boolean verified);

    @Query(nativeQuery = true, value = """
            select count(*) from profile
            where (name_purified, id) < (purify(:lastSeenName), :lastSeenId)
            and (:includeUnverified or not exists (
                select 1 from checker c
                where c.profile_id = profile.id and c.type = :verifyEmail
            ))
            and (:includeReaders or type <> :reader) and id <> :viewerId and (:unfilteredType or type = :type) and (:unfilteredVerified or (exists (select 1 from checker c where c.profile_id = profile.id and c.type = :verifyEmail) = (not :verified)))
            """)
    long countForFindAllDescendingAfter(
            final @Param("lastSeenName") String lastSeenName,
            final @Param("lastSeenId") UUID lastSeenId,
            final @Param("includeUnverified") boolean includeUnverified,
            final @Param("verifyEmail") short verifyEmail,
            final @Param("includeReaders") boolean includeReaders,
            final @Param("reader") short reader,
            final @Param("viewerId") UUID viewerId,
            final @Param("unfilteredType") boolean unfilteredType,
            final @Param("type") short type,
            final @Param("unfilteredVerified") boolean unfilteredVerified,
            final @Param("verified") boolean verified);

    @Query(nativeQuery = true, value = """
            select count(*) from profile
            where
                name_purified like '%' || purify(:name) || '%'
                and (:includeUnverified or not exists (
                    select 1 from checker c
                    where c.profile_id = profile.id and c.type = :verifyEmail
                ))
                and (:includeReaders or type <> :reader) and id <> :viewerId and (:unfilteredType or type = :type) and (:unfilteredVerified or (exists (select 1 from checker c where c.profile_id = profile.id and c.type = :verifyEmail) = (not :verified)))
            """)
    long countForFindByNameContainingIgnoreCase(
            final @Param("name") String name,
            final @Param("includeUnverified") boolean includeUnverified,
            final @Param("verifyEmail") short verifyEmail,
            final @Param("includeReaders") boolean includeReaders,
            final @Param("reader") short reader,
            final @Param("viewerId") UUID viewerId,
            final @Param("unfilteredType") boolean unfilteredType,
            final @Param("type") short type,
            final @Param("unfilteredVerified") boolean unfilteredVerified,
            final @Param("verified") boolean verified);

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
                and (:includeReaders or type <> :reader) and id <> :viewerId and (:unfilteredType or type = :type) and (:unfilteredVerified or (exists (select 1 from checker c where c.profile_id = profile.id and c.type = :verifyEmail) = (not :verified)))
            """)
    long countForFindByNameContainingIgnoreCaseAscendingAfter(
            final @Param("lastSeenName") String lastSeenName,
            final @Param("lastSeenId") UUID lastSeenId,
            final @Param("name") String name,
            final @Param("includeUnverified") boolean includeUnverified,
            final @Param("verifyEmail") short verifyEmail,
            final @Param("includeReaders") boolean includeReaders,
            final @Param("reader") short reader,
            final @Param("viewerId") UUID viewerId,
            final @Param("unfilteredType") boolean unfilteredType,
            final @Param("type") short type,
            final @Param("unfilteredVerified") boolean unfilteredVerified,
            final @Param("verified") boolean verified);

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
                and (:includeReaders or type <> :reader) and id <> :viewerId and (:unfilteredType or type = :type) and (:unfilteredVerified or (exists (select 1 from checker c where c.profile_id = profile.id and c.type = :verifyEmail) = (not :verified)))
            """)
    long countForFindByNameContainingIgnoreCaseDescendingAfter(
            final @Param("lastSeenName") String lastSeenName,
            final @Param("lastSeenId") UUID lastSeenId,
            final @Param("name") String name,
            final @Param("includeUnverified") boolean includeUnverified,
            final @Param("verifyEmail") short verifyEmail,
            final @Param("includeReaders") boolean includeReaders,
            final @Param("reader") short reader,
            final @Param("viewerId") UUID viewerId,
            final @Param("unfilteredType") boolean unfilteredType,
            final @Param("type") short type,
            final @Param("unfilteredVerified") boolean unfilteredVerified,
            final @Param("verified") boolean verified);

    default List<ProfileJpaEntity> findAllAscending(final int limit, final UUID viewerId) {
        return findAllAscending(limit, true, verifyEmailType(), true, readerType(), viewerId, true, (short) 0, true, false);
    }

    default List<ProfileJpaEntity> findAllAscendingAfter(
            final int limit,
            final String lastSeenName,
            final UUID lastSeenId,
            final UUID viewerId) {
        return findAllAscendingAfter(
                limit,
                lastSeenName,
                lastSeenId,
                true,
                verifyEmailType(),
                true,
                readerType(),
                viewerId,
                true,
                (short) 0,
                true,
                false);
    }

    default List<ProfileJpaEntity> findAllDescending(final int limit, final UUID viewerId) {
        return findAllDescending(limit, true, verifyEmailType(), true, readerType(), viewerId, true, (short) 0, true, false);
    }

    default List<ProfileJpaEntity> findAllDescendingAfter(
            final int limit,
            final String lastSeenName,
            final UUID lastSeenId,
            final UUID viewerId) {
        return findAllDescendingAfter(
                limit,
                lastSeenName,
                lastSeenId,
                true,
                verifyEmailType(),
                true,
                readerType(),
                viewerId,
                true,
                (short) 0,
                true,
                false);
    }

    default List<ProfileJpaEntity> findByNameContainingIgnoreCaseAscending(
            final int limit,
            final String name,
            final UUID viewerId) {
        return findByNameContainingIgnoreCaseAscending(
                limit,
                name,
                true,
                verifyEmailType(),
                true,
                readerType(),
                viewerId,
                true,
                (short) 0,
                true,
                false);
    }

    default List<ProfileJpaEntity> findByNameContainingIgnoreCaseAscendingAfter(
            final int limit,
            final String lastSeenName,
            final UUID lastSeenId,
            final String name,
            final UUID viewerId) {
        return findByNameContainingIgnoreCaseAscendingAfter(
                limit,
                lastSeenName,
                lastSeenId,
                name,
                true,
                verifyEmailType(),
                true,
                readerType(),
                viewerId,
                true,
                (short) 0,
                true,
                false);
    }

    default List<ProfileJpaEntity> findByNameContainingIgnoreCaseDescending(
            final int limit,
            final String name,
            final UUID viewerId) {
        return findByNameContainingIgnoreCaseDescending(
                limit,
                name,
                true,
                verifyEmailType(),
                true,
                readerType(),
                viewerId,
                true,
                (short) 0,
                true,
                false);
    }

    default List<ProfileJpaEntity> findByNameContainingIgnoreCaseDescendingAfter(
            final int limit,
            final String lastSeenName,
            final UUID lastSeenId,
            final String name,
            final UUID viewerId) {
        return findByNameContainingIgnoreCaseDescendingAfter(
                limit,
                lastSeenName,
                lastSeenId,
                name,
                true,
                verifyEmailType(),
                true,
                readerType(),
                viewerId,
                true,
                (short) 0,
                true,
                false);
    }

    default long countForFindAllAscendingAfter(
            final String lastSeenName,
            final UUID lastSeenId,
            final UUID viewerId) {
        return countForFindAllAscendingAfter(
                lastSeenName,
                lastSeenId,
                true,
                verifyEmailType(),
                true,
                readerType(),
                viewerId,
                true,
                (short) 0,
                true,
                false);
    }

    default long countForFindAllDescendingAfter(
            final String lastSeenName,
            final UUID lastSeenId,
            final UUID viewerId) {
        return countForFindAllDescendingAfter(
                lastSeenName,
                lastSeenId,
                true,
                verifyEmailType(),
                true,
                readerType(),
                viewerId,
                true,
                (short) 0,
                true,
                false);
    }

    default long countForFindByNameContainingIgnoreCaseAscendingAfter(
            final String lastSeenName,
            final UUID lastSeenId,
            final String name,
            final UUID viewerId) {
        return countForFindByNameContainingIgnoreCaseAscendingAfter(
                lastSeenName,
                lastSeenId,
                name,
                true,
                verifyEmailType(),
                true,
                readerType(),
                viewerId,
                true,
                (short) 0,
                true,
                false);
    }

    default long countForFindByNameContainingIgnoreCaseDescendingAfter(
            final String lastSeenName,
            final UUID lastSeenId,
            final String name,
            final UUID viewerId) {
        return countForFindByNameContainingIgnoreCaseDescendingAfter(
                lastSeenName,
                lastSeenId,
                name,
                true,
                verifyEmailType(),
                true,
                readerType(),
                viewerId,
                true,
                (short) 0,
                true,
                false);
    }

    private static short verifyEmailType() {
        return (short) Checker.Type.VERIFY_EMAIL.value();
    }

    private static short readerType() {
        return (short) Profile.Type.READER.value();
    }

}
