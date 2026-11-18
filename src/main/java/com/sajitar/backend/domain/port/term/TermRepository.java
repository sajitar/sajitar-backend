package com.sajitar.backend.domain.port.term;

import java.util.Optional;

import com.sajitar.backend.domain.model.term.Term;

public interface TermRepository {

    Optional<Term> findCurrent();

}
