package com.sajitar.backend.adapter.out.persistence.term;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TermJpaRepository extends JpaRepository<TermJpaEntity, TermId> {

    @Query(nativeQuery = true, value = """
            select * from term
            where id = :id
            order by type
            """)
    List<TermJpaEntity> findLinesById(final @Param("id") UUID id);

    @Query(nativeQuery = true, value = """
            select distinct id from term
            order by id asc
            limit :limit
            """)
    List<UUID> findPageIds(final @Param("limit") int limit);

    @Query(nativeQuery = true, value = """
            select distinct id from term
            where id > :lastSeenId
            order by id asc
            limit :limit
            """)
    List<UUID> findPageIdsAfter(
            final @Param("lastSeenId") UUID lastSeenId,
            final @Param("limit") int limit);

    @Query(nativeQuery = true, value = """
            select distinct id from term
            order by id desc
            limit :limit
            """)
    List<UUID> findPageIdsDescending(final @Param("limit") int limit);

    @Query(nativeQuery = true, value = """
            select distinct id from term
            where id < :lastSeenId
            order by id desc
            limit :limit
            """)
    List<UUID> findPageIdsDescendingAfter(
            final @Param("lastSeenId") UUID lastSeenId,
            final @Param("limit") int limit);

    @Query(nativeQuery = true, value = """
            select distinct id from term
            where type = CAST(:type AS term_type)
            order by id asc
            limit :limit
            """)
    List<UUID> findPageIdsByType(
            final @Param("type") String type,
            final @Param("limit") int limit);

    @Query(nativeQuery = true, value = """
            select distinct id from term
            where type = CAST(:type AS term_type)
              and id > :lastSeenId
            order by id asc
            limit :limit
            """)
    List<UUID> findPageIdsByTypeAfter(
            final @Param("type") String type,
            final @Param("lastSeenId") UUID lastSeenId,
            final @Param("limit") int limit);

    @Query(nativeQuery = true, value = """
            select distinct id from term
            where type = CAST(:type AS term_type)
            order by id desc
            limit :limit
            """)
    List<UUID> findPageIdsByTypeDescending(
            final @Param("type") String type,
            final @Param("limit") int limit);

    @Query(nativeQuery = true, value = """
            select distinct id from term
            where type = CAST(:type AS term_type)
              and id < :lastSeenId
            order by id desc
            limit :limit
            """)
    List<UUID> findPageIdsByTypeDescendingAfter(
            final @Param("type") String type,
            final @Param("lastSeenId") UUID lastSeenId,
            final @Param("limit") int limit);

    @Query(nativeQuery = true, value = """
            select count(distinct id) from term
            where id > :lastSeenId
            """)
    long countIdsAfter(final @Param("lastSeenId") UUID lastSeenId);

    @Query(nativeQuery = true, value = """
            select count(distinct id) from term
            where id < :lastSeenId
            """)
    long countIdsBefore(final @Param("lastSeenId") UUID lastSeenId);

    @Query(nativeQuery = true, value = """
            select count(distinct id) from term
            where type = CAST(:type AS term_type)
              and id > :lastSeenId
            """)
    long countIdsByTypeAfter(
            final @Param("type") String type,
            final @Param("lastSeenId") UUID lastSeenId);

    @Query(nativeQuery = true, value = """
            select count(distinct id) from term
            where type = CAST(:type AS term_type)
              and id < :lastSeenId
            """)
    long countIdsByTypeBefore(
            final @Param("type") String type,
            final @Param("lastSeenId") UUID lastSeenId);

    @Query(nativeQuery = true, value = """
            select id from term
            where not draft
            group by id
            having count(distinct type) = 3
            order by id desc
            limit :limit
            """)
    List<UUID> findCurrentIds(final @Param("limit") int limit);

}
