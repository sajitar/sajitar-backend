package com.sajitar.backend.adapter.out.persistence.checker;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sajitar.backend.domain.model.checker.Checker;

public interface CheckerJpaRepository extends JpaRepository<CheckerJpaEntity, UUID> {

    Optional<CheckerJpaEntity> findByProfileIdAndType(UUID profileId, Checker.Type type);

    @Query(nativeQuery = true, value = """
            select * from checker
            where profile_id = :profileId
            order by type asc
            limit :limit
            """)
    List<CheckerJpaEntity> findPageByProfileId(
            final @Param("profileId") UUID profileId,
            final @Param("limit") int limit);

    @Query(nativeQuery = true, value = """
            select * from checker
            where profile_id = :profileId
              and type > :lastSeenType
            order by type asc
            limit :limit
            """)
    List<CheckerJpaEntity> findPageByProfileIdAfter(
            final @Param("profileId") UUID profileId,
            final @Param("lastSeenType") short lastSeenType,
            final @Param("limit") int limit);

}
