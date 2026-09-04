package com.sajitar.backend.adapter.out.persistence.note;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoteJpaRepository extends JpaRepository<NoteJpaEntity, UUID> {

    @Query(nativeQuery = true, value = """
            select * from note
            where profile_id = :profileId
            order by id asc
            limit :limit
            """)
    List<NoteJpaEntity> findPageByProfileId(
            final @Param("profileId") UUID profileId,
            final @Param("limit") int limit);

    @Query(nativeQuery = true, value = """
            select * from note
            where profile_id = :profileId
              and id > :lastSeenId
            order by id asc
            limit :limit
            """)
    List<NoteJpaEntity> findPageByProfileIdAfter(
            final @Param("profileId") UUID profileId,
            final @Param("lastSeenId") UUID lastSeenId,
            final @Param("limit") int limit);

    @Query(nativeQuery = true, value = """
            select * from note
            where profile_id = :profileId
            order by id desc
            limit :limit
            """)
    List<NoteJpaEntity> findPageByProfileIdDescending(
            final @Param("profileId") UUID profileId,
            final @Param("limit") int limit);

    @Query(nativeQuery = true, value = """
            select * from note
            where profile_id = :profileId
              and id < :lastSeenId
            order by id desc
            limit :limit
            """)
    List<NoteJpaEntity> findPageByProfileIdDescendingAfter(
            final @Param("profileId") UUID profileId,
            final @Param("lastSeenId") UUID lastSeenId,
            final @Param("limit") int limit);

    @Query(nativeQuery = true, value = """
            select * from note
            where profile_id = :profileId
              and type = :type
            order by id asc
            limit :limit
            """)
    List<NoteJpaEntity> findPageByProfileIdAndType(
            final @Param("profileId") UUID profileId,
            final @Param("type") short type,
            final @Param("limit") int limit);

    @Query(nativeQuery = true, value = """
            select * from note
            where profile_id = :profileId
              and type = :type
              and id > :lastSeenId
            order by id asc
            limit :limit
            """)
    List<NoteJpaEntity> findPageByProfileIdAndTypeAfter(
            final @Param("profileId") UUID profileId,
            final @Param("type") short type,
            final @Param("lastSeenId") UUID lastSeenId,
            final @Param("limit") int limit);

    @Query(nativeQuery = true, value = """
            select * from note
            where profile_id = :profileId
              and type = :type
            order by id desc
            limit :limit
            """)
    List<NoteJpaEntity> findPageByProfileIdAndTypeDescending(
            final @Param("profileId") UUID profileId,
            final @Param("type") short type,
            final @Param("limit") int limit);

    @Query(nativeQuery = true, value = """
            select * from note
            where profile_id = :profileId
              and type = :type
              and id < :lastSeenId
            order by id desc
            limit :limit
            """)
    List<NoteJpaEntity> findPageByProfileIdAndTypeDescendingAfter(
            final @Param("profileId") UUID profileId,
            final @Param("type") short type,
            final @Param("lastSeenId") UUID lastSeenId,
            final @Param("limit") int limit);

    @Query(nativeQuery = true, value = """
            select count(*) from note
            where profile_id = :profileId
              and id > :lastSeenId
            """)
    long countByProfileIdAndIdAfter(
            final @Param("profileId") UUID profileId,
            final @Param("lastSeenId") UUID lastSeenId);

    @Query(nativeQuery = true, value = """
            select count(*) from note
            where profile_id = :profileId
              and id < :lastSeenId
            """)
    long countByProfileIdAndIdBefore(
            final @Param("profileId") UUID profileId,
            final @Param("lastSeenId") UUID lastSeenId);

    @Query(nativeQuery = true, value = """
            select count(*) from note
            where profile_id = :profileId
              and type = :type
              and id > :lastSeenId
            """)
    long countByProfileIdAndTypeAndIdAfter(
            final @Param("profileId") UUID profileId,
            final @Param("type") short type,
            final @Param("lastSeenId") UUID lastSeenId);

    @Query(nativeQuery = true, value = """
            select count(*) from note
            where profile_id = :profileId
              and type = :type
              and id < :lastSeenId
            """)
    long countByProfileIdAndTypeAndIdBefore(
            final @Param("profileId") UUID profileId,
            final @Param("type") short type,
            final @Param("lastSeenId") UUID lastSeenId);

}
