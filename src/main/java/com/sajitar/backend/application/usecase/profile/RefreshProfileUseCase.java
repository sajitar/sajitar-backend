package com.sajitar.backend.application.usecase.profile;

import org.springframework.stereotype.Service;

import com.sajitar.backend.application.Constraints;
import com.sajitar.backend.application.command.profile.RefreshProfileCommand;
import com.sajitar.backend.domain.exception.EmailNotVerifiedException;
import com.sajitar.backend.domain.exception.InvalidRefreshTokenException;
import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.port.AccessTokenIssuer;
import com.sajitar.backend.domain.port.RefreshTokenParser;
import com.sajitar.backend.domain.port.TokenPair;
import com.sajitar.backend.domain.port.checker.CheckerRepository;
import com.sajitar.backend.domain.port.profile.ProfileRepository;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshProfileUseCase {

    private final ProfileRepository profiles;

    private final CheckerRepository checkers;

    private final RefreshTokenParser refreshTokens;

    private final AccessTokenIssuer tokens;

    private final Validator validator;

    public TokenPair execute(final RefreshProfileCommand command) {
        Constraints.requireValid(validator, command);
        final var profileId = refreshTokens.profileId(command.refreshToken());
        final var profile = profiles.findById(profileId).orElseThrow(InvalidRefreshTokenException::new);
        if (checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL).isPresent()) {
            throw new EmailNotVerifiedException();
        }
        return tokens.issue(profile.id());
    }

}
