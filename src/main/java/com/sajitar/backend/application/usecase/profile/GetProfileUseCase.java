package com.sajitar.backend.application.usecase.profile;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.domain.model.authority.Authority;
import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.model.profile.Profile;
import com.sajitar.backend.domain.port.authority.AuthorityRepository;
import com.sajitar.backend.domain.port.checker.CheckerRepository;
import com.sajitar.backend.domain.port.profile.ProfileRepository;

import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetProfileUseCase {

    private final ProfileRepository profiles;

    private final AuthorityRepository authorities;

    private final CheckerRepository checkers;

    private final Validator validator;

    public Optional<Profile> execute(final UUID id, final UUID viewerProfileId) {
        Constraints.requireValid(validator, new IdQuery(id, viewerProfileId));
        final var found = profiles.findById(id);
        if (found.isEmpty()) {
            return found;
        }
        if (authorities.findByProfileIdAndType(viewerProfileId, Authority.Type.MASTER).isPresent()) {
            return found;
        }
        if (checkers.findByProfileIdAndType(id, Checker.Type.VERIFY_EMAIL).isPresent()) {
            return Optional.empty();
        }
        return found;
    }

    private record IdQuery(@NotNull UUID id, @NotNull UUID viewerProfileId) {
    }

}
