package com.sajitar.backend.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sajitar.backend.application.command.CreateProfileCommand;
import com.sajitar.backend.domain.exception.EmailAlreadyRegisteredException;
import com.sajitar.backend.domain.model.Profile;
import com.sajitar.backend.domain.port.PasswordHasher;
import com.sajitar.backend.domain.port.ProfileRepository;
import com.sajitar.backend.domain.validation.Limit;
import com.sajitar.backend.domain.validation.profile.Birthday;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateProfileUseCase")
class CreateProfileUseCaseTest {

    @Mock
    private ProfileRepository profiles;

    @Mock
    private PasswordHasher passwordHasher;

    private CreateProfileUseCase useCase;

    @BeforeAll
    static void configureValidation() {
        Birthday.BirthdayValidator.configure(18);
        Limit.LimitValidator.configure(100);
    }

    @BeforeEach
    void setUp() {
        useCase = new CreateProfileUseCase(profiles, passwordHasher, ProfileUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Codifica a senha e persiste quando o e-mail não está registrado")
    void hashesPasswordAndPersistsWhenEmailIsFree() {
        final var command = ProfileUseCaseFixture.validCreateCommand();
        when(profiles.findByEmail(command.email())).thenReturn(Optional.empty());
        when(passwordHasher.hash(command.password())).thenReturn("$2a$encoded");
        when(profiles.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command);

        assertThat(saved.password()).isEqualTo("$2a$encoded");
        assertThat(saved.email()).isEqualTo(command.email());
        assertThat(saved.id()).isNotNull();
        verify(passwordHasher).hash(command.password());
        final var captor = ArgumentCaptor.forClass(Profile.class);
        verify(profiles).save(captor.capture());
        assertThat(captor.getValue().password()).isEqualTo("$2a$encoded");
        verify(profiles).findByEmail(command.email());
        verifyNoMoreInteractions(profiles);
    }

    @Test
    @DisplayName("Lança EmailAlreadyRegisteredException quando o e-mail já existe")
    void throwsWhenEmailIsAlreadyRegistered() {
        final var command = ProfileUseCaseFixture.validCreateCommand();
        when(profiles.findByEmail(command.email())).thenReturn(Optional.of(ProfileUseCaseFixture.persistedProfile()));

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(EmailAlreadyRegisteredException.class);
        final var ex = (EmailAlreadyRegisteredException) thrown;
        assertThat(ex.content()).containsKey("email");
        assertThat(ex.content().get("email")).containsExactly(EmailAlreadyRegisteredException.MESSAGE_KEY);
        verify(profiles).findByEmail(command.email());
        verify(passwordHasher, never()).hash(any());
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("Nome inválido: não consulta nem persiste")
    void doesNotTouchRepositoryWhenNameIsInvalid() {
        final var command = new CreateProfileCommand(
                "123",
                ProfileUseCaseFixture.DESCRIPTION,
                ProfileUseCaseFixture.BIRTHDAY,
                ProfileUseCaseFixture.EMAIL,
                ProfileUseCaseFixture.PASSWORD);

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(Pattern.class);
        assertThat(violation.getPropertyPath().toString()).isEqualTo("name");
        verify(profiles, never()).findByEmail(any());
        verify(profiles, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = { "not-an-email", "User@Example.com" })
    @DisplayName("E-mail inválido: não consulta o repositório")
    void doesNotTouchRepositoryWhenEmailIsInvalid(final String email) {
        final var command = new CreateProfileCommand(
                ProfileUseCaseFixture.NAME,
                ProfileUseCaseFixture.DESCRIPTION,
                ProfileUseCaseFixture.BIRTHDAY,
                email,
                ProfileUseCaseFixture.PASSWORD);

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findByEmail(any());
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("Senha curta: não consulta nem hasheia")
    void doesNotTouchRepositoryWhenPasswordIsTooShort() {
        final var command = new CreateProfileCommand(
                ProfileUseCaseFixture.NAME,
                ProfileUseCaseFixture.DESCRIPTION,
                ProfileUseCaseFixture.BIRTHDAY,
                ProfileUseCaseFixture.EMAIL,
                "1234567");

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(Size.class);
        assertThat(violation.getPropertyPath().toString()).isEqualTo("password");
        verify(passwordHasher, never()).hash(any());
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("E-mail nulo: violação @NotNull no command")
    void rejectsNullEmail() {
        final var command = new CreateProfileCommand(
                ProfileUseCaseFixture.NAME,
                ProfileUseCaseFixture.DESCRIPTION,
                ProfileUseCaseFixture.BIRTHDAY,
                null,
                ProfileUseCaseFixture.PASSWORD);

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotNull.class);
        verify(profiles, never()).findByEmail(any());
    }

    @Test
    @DisplayName("Nascimento recente demais: não persiste")
    void rejectsBirthdayBelowMinAge() {
        final var command = new CreateProfileCommand(
                ProfileUseCaseFixture.NAME,
                ProfileUseCaseFixture.DESCRIPTION,
                LocalDate.now().minusYears(10),
                ProfileUseCaseFixture.EMAIL,
                ProfileUseCaseFixture.PASSWORD);

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).save(any());
    }

    @Nested
    @DisplayName("identidade")
    class Identity {

        @Test
        @DisplayName("Gera um id antes de persistir")
        void generatesIdBeforeSave() {
            when(profiles.findByEmail(any())).thenReturn(Optional.empty());
            when(passwordHasher.hash(any())).thenReturn("$2a$encoded");
            when(profiles.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

            final var saved = useCase.execute(ProfileUseCaseFixture.validCreateCommand());

            assertThat(saved.id()).isNotNull();
            verify(profiles).save(eq(saved));
        }

        @Test
        @DisplayName("Perfis com o mesmo id são iguais independentemente dos demais campos")
        void equalsByIdOnly() {
            final var id = UUID.randomUUID();
            final var a = ProfileUseCaseFixture.persistedProfile().withId(id).withName("A");
            final var b = ProfileUseCaseFixture.persistedProfile().withId(id).withName("B");
            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }
    }

}
