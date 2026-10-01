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
            where type = CAST(:changePassword AS checker_type) and id < :cutoffId
            """)
    List<UUID> findChangePasswordCreatedBefore(
            final @Param("changePassword") String changePassword,
            final @Param("cutoffId") UUID cutoffId);

    @Query(nativeQuery = true, value = """
            select id from checker
            where type = CAST(:changeEmail AS checker_type) and id < :cutoffId
            """)
    List<UUID> findChangeEmailCreatedBefore(
            final @Param("changeEmail") String changeEmail,
            final @Param("cutoffId") UUID cutoffId);

    @Query(nativeQuery = true, value = """
            select id from checker
            where type = CAST(:deleteProfile AS checker_type) and id < :cutoffId
            """)
    List<UUID> findDeleteProfileCreatedBefore(
            final @Param("deleteProfile") String deleteProfile,
            final @Param("cutoffId") UUID cutoffId);

    @Query(nativeQuery = true, value = """
            select id from checker
            where type = CAST(:signIn AS checker_type) and id < :cutoffId
            """)
    List<UUID> findSignInCreatedBefore(
            final @Param("signIn") String signIn,
            final @Param("cutoffId") UUID cutoffId);

}
