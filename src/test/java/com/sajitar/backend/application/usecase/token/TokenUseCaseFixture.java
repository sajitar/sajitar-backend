package com.sajitar.backend.application.usecase.token;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.model.profile.Profile;
import com.sajitar.backend.domain.model.token.Client;
import com.sajitar.backend.domain.model.token.IssuedToken;
import com.sajitar.backend.domain.model.token.TokenClaims;
import com.sajitar.backend.domain.model.token.TokenUse;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import lombok.experimental.UtilityClass;

@UtilityClass
final class TokenUseCaseFixture {

    static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    static final UUID PROFILE_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    static final String EMAIL = "user@example.com";

    static final String PASSWORD = "12345678";

    static final String PASSWORD_HASH = "$2a$10$hashedPasswordHashValue012345678901";

    static final String ADDRESS = "203.0.113.10";

    static final Client CLIENT = new Client("Chrome", "Linux", Client.Device.DESKTOP);

    static final long ACCESS_SECONDS = 3600L;

    static final long REFRESH_SECONDS = 604800L;

    static Profile persistedProfile() {
        return new Profile(
                PROFILE_ID,
                "Maria Silva",
                "Uma pessoa criativa e dedicada.",
                LocalDate.parse("1988-01-10"),
                EMAIL,
                PASSWORD_HASH);
    }

    static Checker verifyEmailChecker() {
        return new Checker(
                UUID.fromString("018f3c2a-7b00-7c3d-9e1a-0000000000ff"),
                PROFILE_ID,
                Checker.Type.VERIFY_EMAIL,
                "123456",
                null,
                10,
                3,
                NOW);
    }

    static IssuedToken access() {
        return issued(TokenUse.ACCESS, ACCESS_SECONDS);
    }

    static IssuedToken refresh() {
        return issued(TokenUse.REFRESH, REFRESH_SECONDS);
    }

    static IssuedToken expiredRefresh() {
        return new IssuedToken(TokenClaims.create(TokenUse.REFRESH, NOW, NOW), "eyJ.refresh.expired", 0L);
    }

    private static IssuedToken issued(final TokenUse use, final long expiresInSeconds) {
        return new IssuedToken(
                TokenClaims.create(use, NOW, NOW.plusSeconds(expiresInSeconds)),
                "eyJhbGciOiJIUzI1NiJ9." + use.value() + "." + UUID.randomUUID(),
                expiresInSeconds);
    }

}
