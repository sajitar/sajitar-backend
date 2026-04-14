package com.sajitar.backend.domain.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonView;
import com.sajitar.backend.domain.validation.profile.Birthday;
import com.sajitar.backend.domain.validation.profile.Description;
import com.sajitar.backend.domain.validation.profile.Email;
import com.sajitar.backend.domain.validation.profile.Name;
import com.sajitar.backend.domain.validation.profile.Password;
import com.sajitar.backend.util.Viewer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.With;

@Data
@With
@Builder(toBuilder = true)
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(onConstructor_ = @Deprecated)
@AllArgsConstructor(onConstructor_ = @Deprecated)
@Entity
public class Profile implements Serializable {

    @Id
    @JsonView(Viewer.Public.class)
    private UUID id;

    @Name
    @JsonView(Viewer.Public.class)
    private String name;

    @Description
    @JsonView(Viewer.Public.class)
    private String description;

    @Birthday
    @JsonView(Viewer.Protected.class)
    private LocalDate birthday;

    @Email
    @Column(unique = true)
    @JsonView(Viewer.Protected.class)
    private String email;

    @Password
    @Column(columnDefinition = "char(60)")
    @JsonView(Viewer.Private.class)
    private String password;

}
