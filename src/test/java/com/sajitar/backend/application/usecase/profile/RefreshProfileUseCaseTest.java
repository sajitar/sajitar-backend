package com.sajitar.backend.application.usecase.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sajitar.backend.application.command.profile.RefreshProfileCommand;
import com.sajitar.backend.domain.exception.EmailNotVerifiedException;
import com.sajitar.backend.domain.exception.InvalidRefreshTokenException;
import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.port.AccessToken;
import com.sajitar.backend.domain.port.AccessTokenIssuer;
import com.sajitar.backend.domain.port.RefreshTokenParser;
import com.sajitar.backend.domain.port.TokenPair;
import com.sajitar.backend.domain.port.checker.CheckerRepository;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.validation.Limit;
import com.sajitar.backend.domain.validation.profile.Birthday;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotBlank;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshProfileUseCase")
class RefreshProfileUseCaseTest {

    private static final String REFRESH_TOKEN = "refresh.jwt.value";

    private static final String ACCESS_TOKEN = "access.jwt.value";

    @Mock
    private ProfileRepository profiles;

    @Mock
    private CheckerRepository checkers;

    @Mock
    private RefreshTokenParser refreshTokens;

    @Mock
    private AccessTokenIssuer tokens;

    private RefreshProfileUseCase useCase;

    @BeforeAll
    static void configureValidation() {
        Birthday.BirthdayValidator.configure(18);
        Limit.LimitValidator.configure(100);
    }

    @BeforeEach
    void setUp() {
        useCase = new RefreshProfileUseCase(profiles, checkers, refreshTokens, tokens, ProfileUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Emite um par novo quando o refresh é válido e não persiste o perfil")
    void issuesNewPairWhenRefreshIsValid() {
        final var command = new RefreshProfileCommand(REFRESH_TOKEN);
        final var profile = ProfileUseCaseFixture.persistedProfile();
        final var issued = new TokenPair(new AccessToken("new.access", 3600), new AccessToken("new.refresh", 604800));
        when(refreshTokens.profileId(REFRESH_TOKEN)).thenReturn(profile.id());
        when(profiles.findById(profile.id())).thenReturn(Optional.of(profile));
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL)).thenReturn(Optional.empty());
        when(tokens.issue(profile.id())).thenReturn(issued);

        final var pair = useCase.execute(command);

        assertThat(pair).isEqualTo(issued);
        verify(refreshTokens).profileId(REFRESH_TOKEN);
        verify(profiles).findById(profile.id());
        verify(checkers).findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL);
        verify(tokens).issue(profile.id());
        verify(profiles, never()).save(any());
        verifyNoMoreInteractions(profiles, checkers, refreshTokens, tokens);
    }

    @Test
    @DisplayName("Lança InvalidRefreshTokenException quando o parser rejeita token lixo")
    void throwsWhenParserRejectsGarbage() {
        final var command = new RefreshProfileCommand("not-a-jwt");
        when(refreshTokens.profileId("not-a-jwt")).thenThrow(new InvalidRefreshTokenException());

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertInvalidRefreshToken(thrown);
        verify(refreshTokens).profileId("not-a-jwt");
        verify(profiles, never()).findById(any());
        verify(tokens, never()).issue(any());
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("Lança InvalidRefreshTokenException quando o parser rejeita access no lugar do refresh")
    void throwsWhenParserRejectsAccessToken() {
        final var command = new RefreshProfileCommand(ACCESS_TOKEN);
        when(refreshTokens.profileId(ACCESS_TOKEN)).thenThrow(new InvalidRefreshTokenException());

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertInvalidRefreshToken(thrown);
        verify(refreshTokens).profileId(ACCESS_TOKEN);
        verify(profiles, never()).findById(any());
        verify(tokens, never()).issue(any());
    }

    @Test
    @DisplayName("Lança InvalidRefreshTokenException quando o perfil não existe")
    void throwsWhenProfileIsMissing() {
        final var command = new RefreshProfileCommand(REFRESH_TOKEN);
        final var profileId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        when(refreshTokens.profileId(REFRESH_TOKEN)).thenReturn(profileId);
        when(profiles.findById(profileId)).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertInvalidRefreshToken(thrown);
        verify(profiles).findById(profileId);
        verify(checkers, never()).findByProfileIdAndType(any(), any());
        verify(tokens, never()).issue(any());
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("Lança EmailNotVerifiedException quando existe VERIFY_EMAIL e não emite par")
    void throwsWhenVerifyEmailCheckerExists() {
        final var command = new RefreshProfileCommand(REFRESH_TOKEN);
        final var profile = ProfileUseCaseFixture.persistedProfile();
        when(refreshTokens.profileId(REFRESH_TOKEN)).thenReturn(profile.id());
        when(profiles.findById(profile.id())).thenReturn(Optional.of(profile));
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL))
                .thenReturn(Optional.of(new Checker(
                        profile.id(),
                        profile.id(),
                        Checker.Type.VERIFY_EMAIL,
                        "123456",
                        null,
                        10,
                        3,
                        Instant.parse("2001-04-24T21:00:00Z"))));

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(EmailNotVerifiedException.class);
        final var ex = (EmailNotVerifiedException) thrown;
        assertThat(ex.content()).containsKey("email");
        assertThat(ex.content().get("email")).containsExactly(EmailNotVerifiedException.MESSAGE_KEY);
        verify(checkers).findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL);
        verify(tokens, never()).issue(any());
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("Refresh em branco: não consulta portas")
    void doesNotTouchPortsWhenRefreshTokenIsBlank() {
        final var command = new RefreshProfileCommand("   ");

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotBlank.class);
        assertThat(violation.getPropertyPath().toString()).isEqualTo("refreshToken");
        verify(refreshTokens, never()).profileId(any());
        verify(profiles, never()).findById(any());
        verify(checkers, never()).findByProfileIdAndType(any(), any());
        verify(tokens, never()).issue(any());
    }

    private static void assertInvalidRefreshToken(final Throwable thrown) {
        assertThat(thrown).isInstanceOf(InvalidRefreshTokenException.class);
        final var ex = (InvalidRefreshTokenException) thrown;
        assertThat(ex.content()).containsKey("refreshToken");
        assertThat(ex.content().get("refreshToken")).containsExactly(InvalidRefreshTokenException.MESSAGE_KEY);
    }

}
