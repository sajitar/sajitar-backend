package com.sajitar.backend.domain.model;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.READ_WRITE;
import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import com.sajitar.backend.domain.validation.profile.Birthday;
import com.sajitar.backend.domain.validation.profile.Description;
import com.sajitar.backend.domain.validation.profile.Email;
import com.sajitar.backend.domain.validation.profile.Name;
import com.sajitar.backend.domain.validation.profile.Password;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.With;

@Data
@With
@Entity
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(onConstructor_ = @Deprecated)
@AllArgsConstructor(onConstructor_ = @Deprecated)
public class Profile implements Serializable {

    private static final TimeBasedEpochGenerator ID_GENERATOR = Generators.timeBasedEpochGenerator();

    @Id
    @JsonProperty(access = READ_WRITE)
    private UUID id;

    @Name
    @JsonProperty(access = READ_WRITE)
    private String name;

    @Description
    @JsonProperty(access = READ_WRITE)
    private String description;

    @Birthday
    @JsonProperty(access = WRITE_ONLY)
    private LocalDate birthday;

    @Email
    @Column(unique = true)
    @JsonProperty(access = WRITE_ONLY)
    private String email;

    @Password
    @Column(columnDefinition = "char(60)")
    @JsonProperty(access = WRITE_ONLY)
    private String password;

    @PrePersist
    void assignIdIfAbsent() {
        if (id == null) {
            id = ID_GENERATOR.generate();
        }
    }

}
