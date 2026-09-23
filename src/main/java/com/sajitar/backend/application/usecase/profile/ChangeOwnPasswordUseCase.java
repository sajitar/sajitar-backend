package com.sajitar.backend.application.usecase.profile;

import java.time.Duration;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.command.profile.ChangeOwnPasswordCommand;
import com.sajitar.backend.domain.exception.InvalidCredentialsException;
import com.sajitar.backend.domain.exception.ProfileNotFoundException;
import com.sajitar.backend.domain.exception.TooManyAttemptsException;
import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.model.profile.Profile;
import com.sajitar.backend.domain.model.token.AttemptScope;
import com.sajitar.backend.domain.port.PasswordHasher;
import com.sajitar.backend.domain.port.checker.CheckerRepository;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.port.token.AttemptLimiter;
import com.sajitar.backend.domain.port.token.SessionStore;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChangeOwnPasswordUseCase {

    private final ProfileRepository profiles;

    private final CheckerRepository checkers;

    private final PasswordHasher passwordHasher;

    private final SessionStore sessions;

    private final AttemptLimiter attempts;

    private final Validator validator;

    /**
     * Troca a senha do perfil da sessão. O wipe vem antes da escrita: store fora
     * do ar vira 503 sem trocar a senha, nunca o contrário.
     */
    public void execute(final ChangeOwnPasswordCommand command) {
        Constraints.requireValid(validator, command);
        final var existing = profiles.findById(command.profileId()).orElseThrow(ProfileNotFoundException::new);
        requireCredentials(command.address(), existing.email());
        if (!passwordHasher.matches(command.currentPassword(), existing.password())) {
            throw new InvalidCredentialsException();
        }
        sessions.wipe(existing.id());
        final var hashed = passwordHasher.hash(command.newPassword());
        profiles.save(new Profile(
                existing.id(),
                existing.name(),
                existing.description(),
                existing.birthday(),
                existing.email(),
                hashed));
        checkers.findByProfileIdAndType(existing.id(), Checker.Type.CHANGE_PASSWORD)
                .ifPresent(checker -> checkers.deleteById(checker.id()));
    }

    private void requireCredentials(final String address, final String email) {
        Stream.of(
                attempts.register(AttemptScope.CREDENTIALS, address),
                attempts.register(AttemptScope.CREDENTIALS, email))
                .flatMap(Optional::stream)
                .max(Comparator.naturalOrder())
                .ifPresent(ChangeOwnPasswordUseCase::tooMany);
    }

    private static void tooMany(final Duration retryAfter) {
        throw TooManyAttemptsException.forCredentials(retryAfter);
    }

}
