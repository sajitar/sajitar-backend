package com.sajitar.backend.domain.port.term;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sajitar.backend.domain.model.term.Term;

public interface TermRepository {

    Term save(Term term);

    List<Term> findByIdAll(UUID id);

    Optional<Term> findByIdAndType(UUID id, Term.Type type);

    List<UUID> findPageIds(TermPageCriteria criteria);

    long countAfterCursor(TermPageCriteria criteria);

    Optional<UUID> findCurrentId();

    void deleteById(UUID id);

}
