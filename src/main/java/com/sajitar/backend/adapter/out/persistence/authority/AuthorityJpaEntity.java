package com.sajitar.backend.adapter.out.persistence.authority;

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import com.sajitar.backend.domain.model.authority.Authority;

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
@Table(name = "authority")
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
public class AuthorityJpaEntity implements Serializable {

    private static final TimeBasedEpochGenerator ID_GENERATOR = Generators.timeBasedEpochGenerator();

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID profileId;

    @Convert(converter = AuthorityTypeConverter.class)
    @Column(nullable = false, columnDefinition = "smallint")
    private Authority.Type type;

    @PrePersist
    void assignIdIfAbsent() {
        if (id == null) {
            id = ID_GENERATOR.generate();
        }
    }

}
