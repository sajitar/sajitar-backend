package com.sajitar.backend.adapter.out.persistence.checker;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sajitar.backend.domain.model.checker.Checker;

public interface CheckerJpaRepository extends JpaRepository<CheckerJpaEntity, UUID> {

    Optional<CheckerJpaEntity> findByProfileIdAndType(UUID profileId, Checker.Type type);

}
