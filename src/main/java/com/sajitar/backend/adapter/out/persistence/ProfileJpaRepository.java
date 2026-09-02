package com.sajitar.backend.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfileJpaRepository extends JpaRepository<ProfileJpaEntity, UUID> {

    Optional<ProfileJpaEntity> findByEmail(String email);

    @Query(nativeQuery = true, value = """
            select * from profile
            order by (name_purified, id) asc
            limit :limit
            """)
    List<ProfileJpaEntity> findAllAscending(final @Param("limit") int limit);

    @Query(nativeQuery = true, value = """
            select * from profile
            where (name_purified, id) > (purify(:lastSeenName), :lastSeenId)
            order by (name_purified, id) asc
            limit :limit
            """)
    List<ProfileJpaEntity> findAllAscendingAfter(
            final @Param("limit") int limit,
            final @Param("lastSeenName") String lastSeenName,
            final @Param("lastSeenId") UUID lastSeenId);

    @Query(nativeQuery = true, value = """
            select * from profile
            order by (name_purified, id) desc
            limit :limit
            """)
    List<ProfileJpaEntity> findAllDescending(final @Param("limit") int limit);

    @Query(nativeQuery = true, value = """
            select * from profile
            where (name_purified, id) < (purify(:lastSeenName), :lastSeenId)
            order by (name_purified, id) desc
            limit :limit
            """)
    List<ProfileJpaEntity> findAllDescendingAfter(
            final @Param("limit") int limit,
            final @Param("lastSeenName") String lastSeenName,
            final @Param("lastSeenId") UUID lastSeenId);

    @Query(nativeQuery = true, value = """
            select * from profile
            where
                name_purified like '%' || purify(:name) || '%'
            order by (name_purified, id) asc
            limit :limit
            """)
    List<ProfileJpaEntity> findByNameContainingIgnoreCaseAscending(
            final @Param("limit") int limit,
            final @Param("name") String name);

    @Query(nativeQuery = true, value = """
            select * from profile
            where
                (name_purified, id) > (purify(:lastSeenName), :lastSeenId)
                and
                name_purified like '%' || purify(:name) || '%'
            order by (name_purified, id) asc
            limit :limit
            """)
    List<ProfileJpaEntity> findByNameContainingIgnoreCaseAscendingAfter(
            final @Param("limit") int limit,
            final @Param("lastSeenName") String lastSeenName,
            final @Param("lastSeenId") UUID lastSeenId,
            final @Param("name") String name);

    @Query(nativeQuery = true, value = """
            select * from profile
            where
                name_purified like '%' || purify(:name) || '%'
            order by (name_purified, id) desc
            limit :limit
            """)
    List<ProfileJpaEntity> findByNameContainingIgnoreCaseDescending(
            final @Param("limit") int limit,
            final @Param("name") String name);

    @Query(nativeQuery = true, value = """
            select * from profile
            where
                (name_purified, id) < (purify(:lastSeenName), :lastSeenId)
                and
                name_purified like '%' || purify(:name) || '%'
            order by (name_purified, id) desc
            limit :limit
            """)
    List<ProfileJpaEntity> findByNameContainingIgnoreCaseDescendingAfter(
            final @Param("limit") int limit,
            final @Param("lastSeenName") String lastSeenName,
            final @Param("lastSeenId") UUID lastSeenId,
            final @Param("name") String name);

    @Query(nativeQuery = true, value = "select count(*) from profile")
    long countForFindAll();

    @Query(nativeQuery = true, value = """
            select count(*) from profile
            where (name_purified, id) > (purify(:lastSeenName), :lastSeenId)
            """)
    long countForFindAllAscendingAfter(
            final @Param("lastSeenName") String lastSeenName,
            final @Param("lastSeenId") UUID lastSeenId);

    @Query(nativeQuery = true, value = """
            select count(*) from profile
            where (name_purified, id) < (purify(:lastSeenName), :lastSeenId)
            """)
    long countForFindAllDescendingAfter(
            final @Param("lastSeenName") String lastSeenName,
            final @Param("lastSeenId") UUID lastSeenId);

    @Query(nativeQuery = true, value = """
            select count(*) from profile
            where
                name_purified like '%' || purify(:name) || '%'
            """)
    long countForFindByNameContainingIgnoreCase(final @Param("name") String name);

    @Query(nativeQuery = true, value = """
            select count(*) from profile
            where
                (name_purified, id) > (purify(:lastSeenName), :lastSeenId)
                and
                name_purified like '%' || purify(:name) || '%'
            """)
    long countForFindByNameContainingIgnoreCaseAscendingAfter(
            final @Param("lastSeenName") String lastSeenName,
            final @Param("lastSeenId") UUID lastSeenId,
            final @Param("name") String name);

    @Query(nativeQuery = true, value = """
            select count(*) from profile
            where
                (name_purified, id) < (purify(:lastSeenName), :lastSeenId)
                and
                name_purified like '%' || purify(:name) || '%'
            """)
    long countForFindByNameContainingIgnoreCaseDescendingAfter(
            final @Param("lastSeenName") String lastSeenName,
            final @Param("lastSeenId") UUID lastSeenId,
            final @Param("name") String name);

}
