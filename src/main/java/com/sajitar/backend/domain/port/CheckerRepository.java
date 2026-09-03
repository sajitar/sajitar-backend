package com.sajitar.backend.domain.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sajitar.backend.domain.model.Checker;

public interface CheckerRepository {

    Checker save(Checker checker);

    Optional<Checker> findById(UUID id);

    Optional<Checker> findByProfileIdAndType(UUID profileId, Checker.Type type);

    List<Checker> findPage(CheckerPageCriteria criteria);

    void deleteById(UUID id);

}
