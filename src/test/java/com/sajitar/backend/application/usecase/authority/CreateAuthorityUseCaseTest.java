package com.sajitar.backend.application.usecase.authority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sajitar.backend.application.command.authority.CreateAuthorityCommand;
import com.sajitar.backend.domain.exception.AuthorityTypeAlreadyExistsException;
import com.sajitar.backend.domain.exception.ProfileUnavailableException;
import com.sajitar.backend.domain.model.authority.Authority;
import com.sajitar.backend.domain.port.authority.AuthorityRepository;
import com.sajitar.backend.domain.port.profile.ProfileRepository;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateAuthorityUseCase")
class CreateAuthorityUseCaseTest {

    @Mock
    private AuthorityRepository authorities;

    @Mock
    private ProfileRepository profiles;

    private CreateAuthorityUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateAuthorityUseCase(authorities, profiles, AuthorityUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Persiste MASTER quando o tipo está livre")
    void persistsMaster() {
        final var command = AuthorityUseCaseFixture.validCreateCommand();
        when(profiles.findById(command.profileId())).thenReturn(Optional.of(AuthorityUseCaseFixture.availableProfile()));
        when(authorities.findByProfileIdAndType(command.profileId(), command.type())).thenReturn(Optional.empty());
        when(authorities.save(any(Authority.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command);

        assertThat(saved.type()).isEqualTo(Authority.Type.MASTER);
        assertThat(saved.profileId()).isEqualTo(command.profileId());
        final var captor = ArgumentCaptor.forClass(Authority.class);
        verify(authorities).save(captor.capture());
        assertThat(captor.getValue().profileId()).isEqualTo(command.profileId());
        assertThat(captor.getValue().id()).isNotNull();
    }

    @Test
    @DisplayName("Persiste MEMBER quando o tipo está livre")
    void persistsMember() {
        final var command = new CreateAuthorityCommand(AuthorityUseCaseFixture.PROFILE_ID, Authority.Type.MEMBER);
        when(profiles.findById(command.profileId())).thenReturn(Optional.of(AuthorityUseCaseFixture.availableProfile()));
        when(authorities.findByProfileIdAndType(command.profileId(), command.type())).thenReturn(Optional.empty());
        when(authorities.save(any(Authority.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command);

        assertThat(saved.type()).isEqualTo(Authority.Type.MEMBER);
    }

    @Test
    @DisplayName("Perfil inexistente lança ProfileUnavailableException")
    void throwsWhenProfileIsMissing() {
        final var command = AuthorityUseCaseFixture.validCreateCommand();
        when(profiles.findById(command.profileId())).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(ProfileUnavailableException.class);
        verify(authorities, never()).findByProfileIdAndType(any(), any());
        verify(authorities, never()).save(any());
    }

    @Test
    @DisplayName("Tipo duplicado lança AuthorityTypeAlreadyExistsException")
    void throwsWhenTypeAlreadyExists() {
        final var command = AuthorityUseCaseFixture.validCreateCommand();
        when(profiles.findById(command.profileId())).thenReturn(Optional.of(AuthorityUseCaseFixture.availableProfile()));
        when(authorities.findByProfileIdAndType(command.profileId(), command.type()))
                .thenReturn(Optional.of(AuthorityUseCaseFixture.persistedMaster()));

        final var thrown = catchThrowable(() -> useCase.execute(command));

        assertThat(thrown).isInstanceOf(AuthorityTypeAlreadyExistsException.class);
        assertThat(((AuthorityTypeAlreadyExistsException) thrown).content().get("type"))
                .containsExactly(AuthorityTypeAlreadyExistsException.MESSAGE_KEY);
        verify(authorities, never()).save(any());
        verifyNoMoreInteractions(authorities);
    }

    @Test
    @DisplayName("profileId nulo: não consulta repositórios")
    void rejectsNullProfileId() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new CreateAuthorityCommand(null, Authority.Type.MASTER)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotNull.class);
        verify(profiles, never()).findById(any());
        verify(authorities, never()).save(any());
    }

    @Test
    @DisplayName("type nulo: não consulta repositórios")
    void rejectsNullType() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new CreateAuthorityCommand(AuthorityUseCaseFixture.PROFILE_ID, null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findById(any());
        verify(authorities, never()).save(any());
    }

}
