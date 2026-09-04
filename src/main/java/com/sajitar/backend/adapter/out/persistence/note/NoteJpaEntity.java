package com.sajitar.backend.adapter.out.persistence.note;

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import com.sajitar.backend.domain.model.note.Note;

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
@Table(name = "note")
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
public class NoteJpaEntity implements Serializable {

    private static final TimeBasedEpochGenerator ID_GENERATOR = Generators.timeBasedEpochGenerator();

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID profileId;

    @Convert(converter = TypeConverter.class)
    @Column(nullable = false, columnDefinition = "smallint")
    private Note.Type type;

    @Column(nullable = false, length = 1000)
    private String content;

    @PrePersist
    void assignIdIfAbsent() {
        if (id == null) {
            id = ID_GENERATOR.generate();
        }
    }

}
