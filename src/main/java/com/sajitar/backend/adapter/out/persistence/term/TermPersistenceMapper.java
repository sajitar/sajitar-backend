package com.sajitar.backend.adapter.out.persistence.term;

import com.sajitar.backend.domain.model.term.Term;
import com.sajitar.backend.domain.model.term.TermDetail;

import lombok.experimental.UtilityClass;

@UtilityClass
final class TermPersistenceMapper {

    static Term toDomain(final TermJpaEntity entity) {
        return new Term(entity.getId());
    }

    static TermJpaEntity toTermEntity(final Term term) {
        return TermJpaEntity.builder()
                .id(term.id())
                .build();
    }

    static TermDetail toDomain(final TermDetailJpaEntity entity) {
        return new TermDetail(entity.getTermId(), entity.getType(), entity.getContent(), entity.isDraft());
    }

    static TermDetailJpaEntity toDetailEntity(final TermDetail detail) {
        return TermDetailJpaEntity.builder()
                .termId(detail.termId())
                .type(detail.type())
                .content(detail.content())
                .draft(detail.draft())
                .build();
    }

}
