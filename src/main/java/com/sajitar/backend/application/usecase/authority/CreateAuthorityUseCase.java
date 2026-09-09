package com.sajitar.backend.application.usecase.authority;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.command.authority.CreateAuthorityCommand;
import com.sajitar.backend.domain.exception.AuthorityTypeAlreadyExistsException;
import com.sajitar.backend.domain.exception.ProfileUnavailableException;
import com.sajitar.backend.domain.model.authority.Authority;
import com.sajitar.backend.domain.port.authority.AuthorityRepository;
import com.sajitar.backend.domain.port.profile.ProfileRepository;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateAuthorityUseCase {

    private final AuthorityRepository authorities;

    private final ProfileRepository profiles;

    private final Validator validator;

    public Authority execute(final CreateAuthorityCommand command) {
        Constraints.requireValid(validator, command);
        profiles.findById(command.profileId()).orElseThrow(ProfileUnavailableException::new);
        authorities.findByProfileIdAndType(command.profileId(), command.type()).ifPresent(_ -> {
            throw new AuthorityTypeAlreadyExistsException();
        });
        return authorities.save(Authority.create(command.profileId(), command.type()));
    }

}
