package com.sajitar.backend.adapter.out.persistence.profile;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import com.sajitar.backend.domain.model.profile.Profile;

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
@Table(name = "profile")
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
public class ProfileJpaEntity implements Serializable {

    private static final TimeBasedEpochGenerator ID_GENERATOR = Generators.timeBasedEpochGenerator();

    @Id
    private UUID id;

    @Convert(converter = TypeConverter.class)
    @Column(nullable = false, columnDefinition = "smallint")
    private Profile.Type type;

    private String name;

    private String description;

    private LocalDate birthday;

    private String email;

    @Column(columnDefinition = "char(60)")
    private String password;

    @Column(name = "two_factor", nullable = false, columnDefinition = "boolean not null default false")
    private boolean twoFactor;

    @PrePersist
    void assignIdIfAbsent() {
        if (id == null) {
            id = ID_GENERATOR.generate();
        }
    }

}
