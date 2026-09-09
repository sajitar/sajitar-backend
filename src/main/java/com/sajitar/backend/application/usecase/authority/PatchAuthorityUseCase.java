package com.sajitar.backend.application.usecase.authority;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.command.authority.PatchAuthorityCommand;
import com.sajitar.backend.domain.exception.AuthorityNotFoundException;
import com.sajitar.backend.domain.exception.AuthorityTypeAlreadyExistsException;
import com.sajitar.backend.domain.model.authority.Authority;
import com.sajitar.backend.domain.port.authority.AuthorityRepository;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PatchAuthorityUseCase {

    private final AuthorityRepository authorities;

    private final Validator validator;

    public Authority execute(final PatchAuthorityCommand command) {
        Constraints.requireValid(validator, command);
        final var existing = authorities.findById(command.id()).orElseThrow(AuthorityNotFoundException::new);
        if (!command.hasChanges()) {
            return existing;
        }
        final var nextType = command.type();
        if (existing.type() == nextType) {
            return existing;
        }
        authorities.findByProfileIdAndType(existing.profileId(), nextType).ifPresent(_ -> {
            throw new AuthorityTypeAlreadyExistsException();
        });
        return authorities.save(existing.withType(nextType));
    }

}
