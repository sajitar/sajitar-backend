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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sajitar.backend.application.command.profile.SignInProfileCommand;
import com.sajitar.backend.domain.exception.EmailNotVerifiedException;
import com.sajitar.backend.domain.exception.InvalidCredentialsException;
import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.port.AccessToken;
import com.sajitar.backend.domain.port.AccessTokenIssuer;
import com.sajitar.backend.domain.port.PasswordHasher;
import com.sajitar.backend.domain.port.TokenPair;
import com.sajitar.backend.domain.port.checker.CheckerRepository;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.validation.Limit;
import com.sajitar.backend.domain.validation.profile.Birthday;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@ExtendWith(MockitoExtension.class)
@DisplayName("SignInProfileUseCase")
class SignInProfileUseCaseTest {

    @Mock
    private ProfileRepository profiles;

    @Mock
    private CheckerRepository checkers;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private AccessTokenIssuer tokens;

    private SignInProfileUseCase useCase;

    @BeforeAll
    static void configureValidation() {
        Birthday.BirthdayValidator.configure(18);
        Limit.LimitValidator.configure(100);
    }

    @BeforeEach
    void setUp() {
        useCase = new SignInProfileUseCase(profiles, checkers, passwordHasher, tokens, ProfileUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Emite o par de tokens quando e-mail e senha coincidem e não persiste o perfil")
    void issuesTokenPairWhenCredentialsMatch() {
        final var command = new SignInProfileCommand(ProfileUseCaseFixture.EMAIL, ProfileUseCaseFixture.PASSWORD);
        final var profile = ProfileUseCaseFixture.persistedProfile();
        final var issued = new TokenPair(
                new AccessToken("jwt.access.value", 3600),
                new AccessToken("jwt.refresh.value", 604800));
        when(profiles.findByEmail(command.email())).thenReturn(Optional.of(profile));
        when(passwordHasher.matches(command.password(), profile.password())).thenReturn(true);
        when(checkers.findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL)).thenReturn(Optional.empty());
        when(tokens.issue(profile.id())).thenReturn(issued);

        final var pair = useCase.execute(command);

        assertThat(pair).isEqualTo(issued);
        verify(profiles).findByEmail(command.email());
        verify(passwordHasher).matches(command.password(), profile.password());
        verify(checkers).findByProfileIdAndType(profile.id(), Checker.Type.VERIFY_EMAIL);
        verify(tokens).issue(profile.id());
        verify(profiles, never()).save(any());
        verifyNoMoreInteractions(profiles, checkers, tokens);
    }

    @Test
    @DisplayName("Lança EmailNotVerifiedException quando existe VERIFY_EMAIL e não emite token")
    void throwsWhenVerifyEmailCheckerExists() {
        final var command = new SignInProfileCommand(ProfileUseCaseFixture.EMAIL, ProfileUseCaseFixture.PASSWORD);
        final var profile = ProfileUseCaseFixture.persistedProfile();
        when(profiles.findByEmail(command.email())).thenReturn(Optional.of(profile));
        when(passwordHasher.matches(command.password(), profile.password())).thenReturn(true);
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
    @DisplayName("Lança InvalidCredentialsException quando o e-mail não existe")
    void throwsWhenEmailIsUnknown() {
        final var command = new SignInProfileCommand(ProfileUseCaseFixture.EMAIL, ProfileUseCaseFixture.PASSWORD);
        when(profiles.findByEmail(command.email())).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(InvalidCredentialsException.class);
        final var ex = (InvalidCredentialsException) thrown;
        assertThat(ex.content()).containsKey("credentials");
        assertThat(ex.content().get("credentials")).containsExactly(InvalidCredentialsException.MESSAGE_KEY);
        verify(profiles).findByEmail(command.email());
        verify(passwordHasher, never()).matches(any(), any());
        verify(checkers, never()).findByProfileIdAndType(any(), any());
        verify(tokens, never()).issue(any());
    }

    @Test
    @DisplayName("Lança InvalidCredentialsException quando a senha não coincide")
    void throwsWhenPasswordDoesNotMatch() {
        final var command = new SignInProfileCommand(ProfileUseCaseFixture.EMAIL, ProfileUseCaseFixture.PASSWORD);
        final var profile = ProfileUseCaseFixture.persistedProfile();
        when(profiles.findByEmail(command.email())).thenReturn(Optional.of(profile));
        when(passwordHasher.matches(command.password(), profile.password())).thenReturn(false);

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(InvalidCredentialsException.class);
        verify(passwordHasher).matches(command.password(), profile.password());
        verify(checkers, never()).findByProfileIdAndType(any(), any());
        verify(tokens, never()).issue(any());
        verify(profiles, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = { "not-an-email", "User@Example.com" })
    @DisplayName("E-mail inválido: não consulta repositório nem emite token")
    void doesNotTouchPortsWhenEmailIsInvalid(final String email) {
        final var command = new SignInProfileCommand(email, ProfileUseCaseFixture.PASSWORD);

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findByEmail(any());
        verify(passwordHasher, never()).matches(any(), any());
        verify(checkers, never()).findByProfileIdAndType(any(), any());
        verify(tokens, never()).issue(any());
    }

    @Test
    @DisplayName("Senha curta: não consulta repositório nem emite token")
    void doesNotTouchPortsWhenPasswordIsTooShort() {
        final var command = new SignInProfileCommand(ProfileUseCaseFixture.EMAIL, "1234567");

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(Size.class);
        assertThat(violation.getPropertyPath().toString()).isEqualTo("password");
        verify(profiles, never()).findByEmail(any());
        verify(checkers, never()).findByProfileIdAndType(any(), any());
        verify(tokens, never()).issue(any());
    }

    @Test
    @DisplayName("E-mail nulo: violação @NotNull no command")
    void rejectsNullEmail() {
        final var command = new SignInProfileCommand(null, ProfileUseCaseFixture.PASSWORD);

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotNull.class);
        verify(profiles, never()).findByEmail(any());
        verify(checkers, never()).findByProfileIdAndType(any(), any());
        verify(tokens, never()).issue(any());
    }

}
