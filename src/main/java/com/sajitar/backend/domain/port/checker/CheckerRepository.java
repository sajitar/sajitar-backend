package com.sajitar.backend.domain.port.checker;

import java.util.Optional;
import java.util.UUID;

import com.sajitar.backend.domain.model.checker.Checker;

public interface CheckerRepository {

    Checker save(Checker checker);

    Optional<Checker> findById(UUID id);

    Optional<Checker> findByProfileIdAndType(UUID profileId, Checker.Type type);

    void deleteById(UUID id);

}
