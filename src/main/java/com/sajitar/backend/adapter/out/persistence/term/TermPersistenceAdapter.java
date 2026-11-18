package com.sajitar.backend.adapter.out.persistence.term;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.sajitar.backend.domain.model.term.Term;
import com.sajitar.backend.domain.port.term.TermRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
class TermPersistenceAdapter implements TermRepository {

    private final TermJpaRepository jpa;

    @Override
    public Optional<Term> findCurrent() {
        return jpa.findFirstByOrderByIdDesc().map(TermPersistenceMapper::toDomain);
    }

}
