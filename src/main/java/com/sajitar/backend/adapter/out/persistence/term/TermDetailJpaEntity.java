package com.sajitar.backend.adapter.out.persistence.term;

import java.io.Serializable;
import java.util.UUID;

import com.sajitar.backend.domain.model.term.TermDetail;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@Table(name = "term_detail")
@IdClass(TermId.class)
@EqualsAndHashCode(of = { "termId", "type" })
@NoArgsConstructor
@AllArgsConstructor
public class TermDetailJpaEntity implements Serializable {

    @Id
    private UUID termId;

    @Id
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "term_type")
    private TermDetail.Type type;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(nullable = false)
    private boolean draft;

}
