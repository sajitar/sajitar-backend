package com.sajitar.backend.adapter.out.persistence.term;

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import com.sajitar.backend.domain.model.term.Term;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@Table(name = "term")
@IdClass(TermId.class)
@EqualsAndHashCode(of = { "id", "type" })
@NoArgsConstructor
@AllArgsConstructor
public class TermJpaEntity implements Serializable {

    private static final TimeBasedEpochGenerator ID_GENERATOR = Generators.timeBasedEpochGenerator();

    @Id
    private UUID id;

    @Id
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "term_type")
    private Term.Type type;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(nullable = false)
    private boolean draft;

    @PrePersist
    void assignIdIfAbsent() {
        if (id == null) {
            id = ID_GENERATOR.generate();
        }
    }

}
