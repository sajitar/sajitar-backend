package com.sajitar.backend.domain.port.authority;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sajitar.backend.domain.model.authority.Authority;

public interface AuthorityRepository {

    Authority save(Authority authority);

    Optional<Authority> findById(UUID id);

    Optional<Authority> findByProfileIdAndType(UUID profileId, Authority.Type type);

    List<Authority> findPage(AuthorityPageCriteria criteria);

    long countAfterCursor(AuthorityPageCriteria criteria);

    void deleteById(UUID id);

}
