package com.sajitar.backend.application.usecase.profile;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.domain.exception.ForbiddenProfileDetailsException;
import com.sajitar.backend.domain.model.profile.Profile;
import com.sajitar.backend.domain.port.profile.ProfileRepository;

import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetProfileDetailsUseCase {

    private final ProfileRepository profiles;

    private final Validator validator;

    public Optional<Profile> execute(final UUID id, final UUID viewerProfileId) {
        Constraints.requireValid(validator, new IdQuery(id, viewerProfileId));
        final var found = profiles.findById(id);
        if (found.isEmpty()) {
            return found;
        }
        if (id.equals(viewerProfileId)) {
            return found;
        }
        if (isMaster(viewerProfileId)) {
            return found;
        }
        throw new ForbiddenProfileDetailsException();
    }

    private boolean isMaster(final UUID viewerProfileId) {
        return profiles.findById(viewerProfileId)
                .map(viewer -> viewer.type().includes(Profile.Type.MASTER))
                .orElse(false);
    }

    private record IdQuery(@NotNull UUID id, @NotNull UUID viewerProfileId) {
    }

}
