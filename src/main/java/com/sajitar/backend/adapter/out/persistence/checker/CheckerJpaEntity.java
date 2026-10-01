package com.sajitar.backend.adapter.out.persistence.checker;

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import com.sajitar.backend.domain.model.checker.Checker;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
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
@Table(name = "checker")
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
public class CheckerJpaEntity implements Serializable {

    private static final TimeBasedEpochGenerator ID_GENERATOR = Generators.timeBasedEpochGenerator();

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID profileId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "checker_type")
    private Checker.Type type;

    @Column(nullable = false, columnDefinition = "char(6)")
    private String code;

    @Column(columnDefinition = "text")
    private String payload;

    @PrePersist
    void assignIdIfAbsent() {
        if (id == null) {
            id = ID_GENERATOR.generate();
        }
    }

}
