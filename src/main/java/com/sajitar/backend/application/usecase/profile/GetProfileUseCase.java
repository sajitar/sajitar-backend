package com.sajitar.backend.application.usecase.profile;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.domain.model.profile.Profile;
import com.sajitar.backend.domain.port.profile.ProfileRepository;

import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetProfileUseCase {

    private final ProfileRepository profiles;

    private final Validator validator;

    public Optional<Profile> execute(final UUID id) {
        Constraints.requireValid(validator, new IdQuery(id));
        return profiles.findById(id);
    }

    private record IdQuery(@NotNull UUID id) {
    }

}
