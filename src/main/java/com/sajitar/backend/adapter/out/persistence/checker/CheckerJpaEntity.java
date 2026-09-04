package com.sajitar.backend.adapter.out.persistence.checker;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import com.sajitar.backend.domain.model.checker.Checker;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
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

    @Convert(converter = TypeConverter.class)
    @Column(nullable = false, columnDefinition = "smallint")
    private Checker.Type type;

    @Column(nullable = false, columnDefinition = "char(6)")
    private String code;

    @Column(columnDefinition = "text")
    private String payload;

    @Column(nullable = false, columnDefinition = "smallint")
    private short attempts;

    @Column(nullable = false, columnDefinition = "smallint")
    private short replaces;

    @Column(nullable = false, columnDefinition = "timestamp with time zone")
    private Instant updatedAt;

    @PrePersist
    void assignIdIfAbsent() {
        if (id == null) {
            id = ID_GENERATOR.generate();
        }
    }

}
