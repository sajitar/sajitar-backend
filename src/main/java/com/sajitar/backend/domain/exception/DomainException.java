package com.sajitar.backend.domain.exception;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public abstract sealed class DomainException extends RuntimeException
        permits EmailAlreadyRegisteredException, ProfileNotFoundException,
        ProfileUnavailableException, InvalidCheckerTypeException, AuthorityNotFoundException,
        AuthorityTypeAlreadyExistsException, InvalidAuthorityTypeException, NoteNotFoundException,
        InvalidNoteTypeException, InvalidCredentialsException, EmailNotVerifiedException,
        InvalidCheckerVerificationException,
        InvalidRefreshTokenException, SessionNotFoundException, SessionStoreUnavailableException,
        MailUnavailableException, TooManyAttemptsException {

    private final Map<String, List<String>> content;

    protected DomainException(final Map<String, List<String>> content) {
        this.content = Map.copyOf(content);
    }

}
