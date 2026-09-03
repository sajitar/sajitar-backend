package com.sajitar.backend.application.usecase.authority;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.query.authority.GetAuthorityByProfileAndTypeQuery;
import com.sajitar.backend.domain.model.authority.Authority;
import com.sajitar.backend.domain.port.authority.AuthorityRepository;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetAuthorityByProfileAndTypeUseCase {

    private final AuthorityRepository authorities;

    private final Validator validator;

    public Optional<Authority> execute(final GetAuthorityByProfileAndTypeQuery query) {
        Constraints.requireValid(validator, query);
        return authorities.findByProfileIdAndType(query.profileId(), query.type());
    }

}
