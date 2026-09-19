package com.sajitar.backend.application.usecase.profile;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.command.profile.DeleteProfileCommand;
import com.sajitar.backend.domain.exception.ProfileNotFoundException;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.port.token.SessionStore;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeleteProfileUseCase {

    private final ProfileRepository profiles;

    private final SessionStore sessions;

    private final Validator validator;

    /**
     * Excluir o perfil encerra as sessões na hora: o access não sobrevive até o
     * TTL do store. O wipe vem antes da exclusão para que store fora do ar vire
     * 503 com o perfil intacto.
     */
    public void execute(final DeleteProfileCommand command) {
        Constraints.requireValid(validator, command);
        profiles.findById(command.id()).orElseThrow(ProfileNotFoundException::new);
        sessions.wipe(command.id());
        profiles.deleteById(command.id());
    }

}
