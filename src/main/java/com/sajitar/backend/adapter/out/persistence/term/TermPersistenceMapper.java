package com.sajitar.backend.adapter.out.persistence.term;

import com.sajitar.backend.domain.model.term.Term;

import lombok.experimental.UtilityClass;

@UtilityClass
final class TermPersistenceMapper {

    static Term toDomain(final TermJpaEntity entity) {
        return new Term(entity.getId(), entity.getType(), entity.getContent(), entity.isDraft());
    }

    static TermJpaEntity toEntity(final Term term) {
        return TermJpaEntity.builder()
                .id(term.id())
                .type(term.type())
                .content(term.content())
                .draft(term.draft())
                .build();
    }

}
