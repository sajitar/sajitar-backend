package com.sajitar.backend.application.usecase.profile;

import java.time.Clock;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.VerifyEmailMail;
import com.sajitar.backend.application.command.profile.CreateProfileCommand;
import com.sajitar.backend.configuration.ProfilePurgeProperties;
import com.sajitar.backend.domain.exception.EmailAlreadyRegisteredException;
import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.model.profile.Profile;
import com.sajitar.backend.domain.port.Mailer;
import com.sajitar.backend.domain.port.PasswordHasher;
import com.sajitar.backend.domain.port.checker.CheckerRepository;
import com.sajitar.backend.domain.port.profile.ProfileRepository;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateProfileUseCase {

    private final ProfileRepository profiles;

    private final CheckerRepository checkers;

    private final PasswordHasher passwordHasher;

    private final Mailer mailer;

    private final Clock clock;

    private final ProfilePurgeProperties properties;

    private final MessageSource messageSource;

    private final Validator validator;

    @Transactional
    public Profile execute(final CreateProfileCommand command) {
        Constraints.requireValid(validator, command);
        profiles.findByEmail(command.email()).ifPresent(_ -> {
            throw new EmailAlreadyRegisteredException();
        });
        final var saved = profiles.save(Profile.create(
                command.name(),
                command.description(),
                command.birthday(),
                command.email(),
                passwordHasher.hash(command.password())));
        final var checker = checkers.save(Checker.create(saved.id(), Checker.Type.VERIFY_EMAIL));
        mailer.send(VerifyEmailMail.compose(
                messageSource,
                clock.instant(),
                saved.email(),
                checker.code(),
                properties.unverifiedMaxAgeHours()));
        return saved;
    }

}
