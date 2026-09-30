package com.sajitar.backend.application.usecase.profile;

import java.time.Clock;
import java.util.UUID;

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
    public Profile execute(final CreateProfileCommand command, final UUID viewerProfileId) {
        Constraints.requireValid(validator, command);
        profiles.findByEmail(command.email()).ifPresent(_ -> {
            throw new EmailAlreadyRegisteredException();
        });
        final var type = command.type() != null && isMaster(viewerProfileId)
                ? command.type()
                : Profile.Type.WRITER;
        final var saved = profiles.save(Profile.create(
                type,
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
                properties.unverifiedMaxAgeMinutes()));
        return saved;
    }

    private boolean isMaster(final UUID viewerProfileId) {
        return viewerProfileId != null && profiles.findById(viewerProfileId)
                .map(viewer -> viewer.type().includes(Profile.Type.MASTER))
                .orElse(false);
    }

}
