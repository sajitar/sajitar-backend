package com.sajitar.backend.adapter.out.persistence.term;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TermJpaRepository extends JpaRepository<TermJpaEntity, UUID> {

}
