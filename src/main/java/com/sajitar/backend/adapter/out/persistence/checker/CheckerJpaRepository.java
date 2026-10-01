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
            select id from checker
            where type = :changePassword and id < :cutoffId
            """)
    List<UUID> findChangePasswordCreatedBefore(
            final @Param("changePassword") short changePassword,
            final @Param("cutoffId") UUID cutoffId);

    @Query(nativeQuery = true, value = """
            select id from checker
            where type = :changeEmail and id < :cutoffId
            """)
    List<UUID> findChangeEmailCreatedBefore(
            final @Param("changeEmail") short changeEmail,
            final @Param("cutoffId") UUID cutoffId);

    @Query(nativeQuery = true, value = """
            select id from checker
            where type = :deleteProfile and id < :cutoffId
            """)
    List<UUID> findDeleteProfileCreatedBefore(
            final @Param("deleteProfile") short deleteProfile,
            final @Param("cutoffId") UUID cutoffId);

    @Query(nativeQuery = true, value = """
            select id from checker
            where type = :signIn and id < :cutoffId
            """)
    List<UUID> findSignInCreatedBefore(
            final @Param("signIn") short signIn,
            final @Param("cutoffId") UUID cutoffId);

}
