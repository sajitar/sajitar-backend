package com.sajitar.backend.domain.port.profile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sajitar.backend.domain.model.profile.Profile;

public interface ProfileRepository {

    Profile save(Profile profile);

    Optional<Profile> findById(UUID id);

    Optional<Profile> findByEmail(String email);

    void deleteById(UUID id);

    List<Profile> findPage(ProfilePageCriteria criteria);

    long countAfterCursor(ProfilePageCriteria criteria);

}
