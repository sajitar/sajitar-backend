package com.sajitar.backend.adapter.out.persistence.authority;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sajitar.backend.domain.model.authority.Authority;

public interface AuthorityJpaRepository extends JpaRepository<AuthorityJpaEntity, UUID> {

    Optional<AuthorityJpaEntity> findByProfileIdAndType(UUID profileId, Authority.Type type);

    @Query(nativeQuery = true, value = """
            select * from authority
            where profile_id = :profileId
            order by type asc
            limit :limit
            """)
    List<AuthorityJpaEntity> findPageByProfileId(
            final @Param("profileId") UUID profileId,
            final @Param("limit") int limit);

    @Query(nativeQuery = true, value = """
            select * from authority
            where profile_id = :profileId
              and type > :lastSeenType
            order by type asc
            limit :limit
            """)
    List<AuthorityJpaEntity> findPageByProfileIdAfter(
            final @Param("profileId") UUID profileId,
            final @Param("lastSeenType") short lastSeenType,
            final @Param("limit") int limit);

    @Query(nativeQuery = true, value = """
            select * from authority
            where profile_id = :profileId
            order by type desc
            limit :limit
            """)
    List<AuthorityJpaEntity> findPageByProfileIdDescending(
            final @Param("profileId") UUID profileId,
            final @Param("limit") int limit);

    @Query(nativeQuery = true, value = """
            select * from authority
            where profile_id = :profileId
              and type < :lastSeenType
            order by type desc
            limit :limit
            """)
    List<AuthorityJpaEntity> findPageByProfileIdDescendingAfter(
            final @Param("profileId") UUID profileId,
            final @Param("lastSeenType") short lastSeenType,
            final @Param("limit") int limit);

    @Query(nativeQuery = true, value = """
            select count(*) from authority
            where profile_id = :profileId
              and type > :lastSeenType
            """)
    long countByProfileIdAndTypeAfter(
            final @Param("profileId") UUID profileId,
            final @Param("lastSeenType") short lastSeenType);

    @Query(nativeQuery = true, value = """
            select count(*) from authority
            where profile_id = :profileId
              and type < :lastSeenType
            """)
    long countByProfileIdAndTypeBefore(
            final @Param("profileId") UUID profileId,
            final @Param("lastSeenType") short lastSeenType);

}
