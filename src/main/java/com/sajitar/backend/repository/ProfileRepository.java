package com.sajitar.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sajitar.backend.domain.model.Profile;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    Optional<Profile> findByEmail(final String email);

    @Query(nativeQuery = true, value = """
            select * from profile
            order by (name_purified, id) asc
            limit :limit
            """)
    List<Profile> findAllAscending(final @Param("limit") int limit);

    @Query(nativeQuery = true, value = """
            select * from profile
            where (name_purified, id) > (purify(:lastSeenName), :lastSeenId)
            order by (name_purified, id) asc
            limit :limit
            """)
    List<Profile> findAllAscendingAfter(
            final @Param("limit") int limit,
            final @Param("lastSeenName") String lastSeenName,
            final @Param("lastSeenId") UUID lastSeenId);

    @Query(nativeQuery = true, value = """
            select * from profile
            order by (name_purified, id) desc
            limit :limit
            """)
    List<Profile> findAllDescending(final @Param("limit") int limit);

    @Query(nativeQuery = true, value = """
            select * from profile
            where (name_purified, id) < (purify(:lastSeenName), :lastSeenId)
            order by (name_purified, id) desc
            limit :limit
            """)
    List<Profile> findAllDescendingAfter(
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
    List<Profile> findByNameContainingIgnoreCaseAscending(
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
    List<Profile> findByNameContainingIgnoreCaseAscendingAfter(
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
    List<Profile> findByNameContainingIgnoreCaseDescending(
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
    List<Profile> findByNameContainingIgnoreCaseDescendingAfter(
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
