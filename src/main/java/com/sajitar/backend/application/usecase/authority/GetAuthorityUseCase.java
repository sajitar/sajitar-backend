package com.sajitar.backend.application.usecase.authority;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.domain.model.authority.Authority;
import com.sajitar.backend.domain.port.authority.AuthorityRepository;

import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetAuthorityUseCase {

    private final AuthorityRepository authorities;

    private final Validator validator;

    public Optional<Authority> execute(final UUID id) {
        Constraints.requireValid(validator, new IdQuery(id));
        return authorities.findById(id);
    }

    private record IdQuery(@NotNull(message = "{validation.not-null}") UUID id) {
    }

}
