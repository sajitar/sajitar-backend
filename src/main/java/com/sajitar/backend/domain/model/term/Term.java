package com.sajitar.backend.domain.model.term;

import java.util.UUID;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;

public record Term(UUID id) {

    private static final TimeBasedEpochGenerator ID_GENERATOR = Generators.timeBasedEpochGenerator();

    public static Term create() {
        return new Term(ID_GENERATOR.generate());
    }

}
