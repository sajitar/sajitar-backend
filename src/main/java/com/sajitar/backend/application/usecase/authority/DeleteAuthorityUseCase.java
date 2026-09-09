package com.sajitar.backend.application.usecase.authority;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.command.authority.DeleteAuthorityCommand;
import com.sajitar.backend.domain.exception.AuthorityNotFoundException;
import com.sajitar.backend.domain.port.authority.AuthorityRepository;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeleteAuthorityUseCase {

    private final AuthorityRepository authorities;

    private final Validator validator;

    public void execute(final DeleteAuthorityCommand command) {
        Constraints.requireValid(validator, command);
        authorities.findById(command.id()).orElseThrow(AuthorityNotFoundException::new);
        authorities.deleteById(command.id());
    }

}
