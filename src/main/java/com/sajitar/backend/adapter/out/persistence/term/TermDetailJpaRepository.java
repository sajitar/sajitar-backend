package com.sajitar.backend.adapter.out.persistence.term;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TermDetailJpaRepository extends JpaRepository<TermDetailJpaEntity, TermId> {

    @Query(nativeQuery = true, value = """
            select * from term_detail
            where term_id = :id
            order by type
            """)
    List<TermDetailJpaEntity> findLinesById(final @Param("id") UUID id);

    @Query(nativeQuery = true, value = """
            select distinct t.id from term t
            join term_detail d on d.term_id = t.id
            order by t.id asc
            limit :limit
            """)
    List<UUID> findPageIds(final @Param("limit") int limit);

    @Query(nativeQuery = true, value = """
            select distinct t.id from term t
            join term_detail d on d.term_id = t.id
            where t.id > :lastSeenId
            order by t.id asc
            limit :limit
            """)
    List<UUID> findPageIdsAfter(
            final @Param("lastSeenId") UUID lastSeenId,
            final @Param("limit") int limit);

    @Query(nativeQuery = true, value = """
            select distinct t.id from term t
            join term_detail d on d.term_id = t.id
            order by t.id desc
            limit :limit
            """)
    List<UUID> findPageIdsDescending(final @Param("limit") int limit);

    @Query(nativeQuery = true, value = """
            select distinct t.id from term t
            join term_detail d on d.term_id = t.id
            where t.id < :lastSeenId
            order by t.id desc
            limit :limit
            """)
    List<UUID> findPageIdsDescendingAfter(
            final @Param("lastSeenId") UUID lastSeenId,
            final @Param("limit") int limit);

    @Query(nativeQuery = true, value = """
            select distinct t.id from term t
            join term_detail d on d.term_id = t.id
            where d.type = CAST(:type AS term_type)
            order by t.id asc
            limit :limit
            """)
    List<UUID> findPageIdsByType(
            final @Param("type") String type,
            final @Param("limit") int limit);

    @Query(nativeQuery = true, value = """
            select distinct t.id from term t
            join term_detail d on d.term_id = t.id
            where d.type = CAST(:type AS term_type)
              and t.id > :lastSeenId
            order by t.id asc
            limit :limit
            """)
    List<UUID> findPageIdsByTypeAfter(
            final @Param("type") String type,
            final @Param("lastSeenId") UUID lastSeenId,
            final @Param("limit") int limit);

    @Query(nativeQuery = true, value = """
            select distinct t.id from term t
            join term_detail d on d.term_id = t.id
            where d.type = CAST(:type AS term_type)
            order by t.id desc
            limit :limit
            """)
    List<UUID> findPageIdsByTypeDescending(
            final @Param("type") String type,
            final @Param("limit") int limit);

    @Query(nativeQuery = true, value = """
            select distinct t.id from term t
            join term_detail d on d.term_id = t.id
            where d.type = CAST(:type AS term_type)
              and t.id < :lastSeenId
            order by t.id desc
            limit :limit
            """)
    List<UUID> findPageIdsByTypeDescendingAfter(
            final @Param("type") String type,
            final @Param("lastSeenId") UUID lastSeenId,
            final @Param("limit") int limit);

    @Query(nativeQuery = true, value = """
            select count(distinct t.id) from term t
            join term_detail d on d.term_id = t.id
            where t.id > :lastSeenId
            """)
    long countIdsAfter(final @Param("lastSeenId") UUID lastSeenId);

    @Query(nativeQuery = true, value = """
            select count(distinct t.id) from term t
            join term_detail d on d.term_id = t.id
            where t.id < :lastSeenId
            """)
    long countIdsBefore(final @Param("lastSeenId") UUID lastSeenId);

    @Query(nativeQuery = true, value = """
            select count(distinct t.id) from term t
            join term_detail d on d.term_id = t.id
            where d.type = CAST(:type AS term_type)
              and t.id > :lastSeenId
            """)
    long countIdsByTypeAfter(
            final @Param("type") String type,
            final @Param("lastSeenId") UUID lastSeenId);

    @Query(nativeQuery = true, value = """
            select count(distinct t.id) from term t
            join term_detail d on d.term_id = t.id
            where d.type = CAST(:type AS term_type)
              and t.id < :lastSeenId
            """)
    long countIdsByTypeBefore(
            final @Param("type") String type,
            final @Param("lastSeenId") UUID lastSeenId);

    @Query(nativeQuery = true, value = """
            select term_id from term_detail
            where not draft
            group by term_id
            having count(distinct type) = 3
            order by term_id desc
            limit :limit
            """)
    List<UUID> findCurrentIds(final @Param("limit") int limit);

}
