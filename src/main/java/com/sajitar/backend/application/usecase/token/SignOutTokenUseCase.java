package com.sajitar.backend.application.usecase.token;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.command.token.SignOutTokenCommand;
import com.sajitar.backend.domain.exception.InvalidCredentialsException;
import com.sajitar.backend.domain.exception.SessionNotFoundException;
import com.sajitar.backend.domain.exception.TooManyAttemptsException;
import com.sajitar.backend.domain.model.token.AttemptScope;
import com.sajitar.backend.domain.port.PasswordHasher;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.port.token.AttemptLimiter;
import com.sajitar.backend.domain.port.token.SessionStore;
import com.sajitar.backend.domain.validation.profile.Password;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SignOutTokenUseCase {

    private final ProfileRepository profiles;

    private final PasswordHasher passwordHasher;

    private final SessionStore sessions;

    private final AttemptLimiter attempts;

    private final Validator validator;

    /**
     * Encerra as sessões do lote, tudo ou nada. Sair da sessão corrente basta o
     * Bearer; qualquer id de outra sessão exige a senha do perfil, conferida antes
     * de revelar se aquelas sessões existem.
     */
    public void execute(final SignOutTokenCommand command) {
        Constraints.requireValid(validator, command);
        if (command.requiresPassword()) {
            attempts.register(AttemptScope.CREDENTIALS, command.address())
                    .ifPresent(retryAfter -> {
                        throw TooManyAttemptsException.forCredentials(retryAfter);
                    });
            Password.Validation.validate(validator, command.password());
            final var profile = profiles.findById(command.profileId()).orElseThrow(InvalidCredentialsException::new);
            if (!passwordHasher.matches(command.password(), profile.password())) {
                throw new InvalidCredentialsException();
            }
        }
        if (!sessions.close(command.profileId(), command.ids())) {
            throw new SessionNotFoundException();
        }
    }

}
