package com.sajitar.backend.adapter.out.persistence.checker;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.port.checker.CheckerRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
class CheckerPersistenceAdapter implements CheckerRepository {

    private final CheckerJpaRepository jpa;

    @Override
    public Checker save(final Checker checker) {
        return CheckerPersistenceMapper.toDomain(jpa.save(CheckerPersistenceMapper.toEntity(checker)));
    }

    @Override
    public Optional<Checker> findById(final UUID id) {
        return jpa.findById(id).map(CheckerPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Checker> findByProfileIdAndType(final UUID profileId, final Checker.Type type) {
        return jpa.findByProfileIdAndType(profileId, type).map(CheckerPersistenceMapper::toDomain);
    }

    @Override
    public void deleteById(final UUID id) {
        jpa.deleteById(id);
    }

}
