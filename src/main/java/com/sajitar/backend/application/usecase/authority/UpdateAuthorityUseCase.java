package com.sajitar.backend.application.usecase.authority;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.command.authority.UpdateAuthorityCommand;
import com.sajitar.backend.domain.exception.AuthorityNotFoundException;
import com.sajitar.backend.domain.exception.AuthorityTypeAlreadyExistsException;
import com.sajitar.backend.domain.model.authority.Authority;
import com.sajitar.backend.domain.port.authority.AuthorityRepository;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdateAuthorityUseCase {

    private final AuthorityRepository authorities;

    private final Validator validator;

    public Authority execute(final UpdateAuthorityCommand command) {
        Constraints.requireValid(validator, command);
        final var existing = authorities.findById(command.id()).orElseThrow(AuthorityNotFoundException::new);
        return persistIfChanged(existing, command.type());
    }

    private Authority persistIfChanged(final Authority existing, final Authority.Type nextType) {
        if (existing.type() == nextType) {
            return existing;
        }
        authorities.findByProfileIdAndType(existing.profileId(), nextType).ifPresent(_ -> {
            throw new AuthorityTypeAlreadyExistsException();
        });
        return authorities.save(existing.withType(nextType));
    }

}
