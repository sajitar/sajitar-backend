package com.sajitar.backend.domain.port.term;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sajitar.backend.domain.model.term.Term;
import com.sajitar.backend.domain.model.term.TermDetail;

public interface TermRepository {

    TermDetail save(Term term, TermDetail detail);

    List<TermDetail> findByIdAll(UUID id);

    Optional<TermDetail> findByIdAndType(UUID id, TermDetail.Type type);

    List<UUID> findPageIds(TermPageCriteria criteria);

    long countAfterCursor(TermPageCriteria criteria);

    Optional<UUID> findCurrentId();

    void deleteById(UUID id);

}
