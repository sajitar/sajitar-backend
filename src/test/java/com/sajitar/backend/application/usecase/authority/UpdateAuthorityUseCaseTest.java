package com.sajitar.backend.application.usecase.authority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sajitar.backend.application.command.authority.UpdateAuthorityCommand;
import com.sajitar.backend.domain.exception.AuthorityNotFoundException;
import com.sajitar.backend.domain.exception.AuthorityTypeAlreadyExistsException;
import com.sajitar.backend.domain.model.authority.Authority;
import com.sajitar.backend.domain.port.authority.AuthorityRepository;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateAuthorityUseCase")
class UpdateAuthorityUseCaseTest {

    @Mock
    private AuthorityRepository authorities;

    private UpdateAuthorityUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateAuthorityUseCase(authorities, AuthorityUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Troca de tipo livre persiste com o type novo e id/profileId iguais")
    void appliesTypeChange() {
        final var existing = AuthorityUseCaseFixture.persistedMaster();
        when(authorities.findById(existing.id())).thenReturn(Optional.of(existing));
        when(authorities.findByProfileIdAndType(existing.profileId(), Authority.Type.READER))
                .thenReturn(Optional.empty());
        when(authorities.save(any(Authority.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(new UpdateAuthorityCommand(existing.id(), Authority.Type.READER));

        assertThat(saved.id()).isEqualTo(existing.id());
        assertThat(saved.profileId()).isEqualTo(existing.profileId());
        assertThat(saved.type()).isEqualTo(Authority.Type.READER);
        final var captor = ArgumentCaptor.forClass(Authority.class);
        verify(authorities).save(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo(existing.id());
        verify(authorities).findByProfileIdAndType(existing.profileId(), Authority.Type.READER);
    }

    @Test
    @DisplayName("Valores idênticos não persistem")
    void identicalValuesDoNotSave() {
        final var existing = AuthorityUseCaseFixture.persistedMaster();
        when(authorities.findById(existing.id())).thenReturn(Optional.of(existing));

        final var saved = useCase.execute(AuthorityUseCaseFixture.identicalUpdateCommand());

        assertThat(saved).isEqualTo(existing);
        verify(authorities, never()).save(any());
        verify(authorities, never()).findByProfileIdAndType(any(), any());
    }

    @Test
    @DisplayName("Troca para tipo já existente no perfil lança 409")
    void rejectsDuplicateType() {
        final var existing = AuthorityUseCaseFixture.persistedMaster();
        when(authorities.findById(existing.id())).thenReturn(Optional.of(existing));
        when(authorities.findByProfileIdAndType(existing.profileId(), Authority.Type.READER))
                .thenReturn(Optional.of(AuthorityUseCaseFixture.persistedReader()));

        final var thrown = catchThrowable(() -> useCase.execute(
                new UpdateAuthorityCommand(existing.id(), Authority.Type.READER)));

        assertThat(thrown).isInstanceOf(AuthorityTypeAlreadyExistsException.class);
        verify(authorities, never()).save(any());
    }

    @Test
    @DisplayName("Lança AuthorityNotFoundException quando o id não existe")
    void throwsWhenMissing() {
        when(authorities.findById(AuthorityUseCaseFixture.ID)).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(AuthorityUseCaseFixture.identicalUpdateCommand()));

        assertThat(thrown).isInstanceOf(AuthorityNotFoundException.class);
        verify(authorities, never()).save(any());
    }

    @Test
    @DisplayName("type nulo: não consulta o repositório")
    void rejectsNullType() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new UpdateAuthorityCommand(AuthorityUseCaseFixture.ID, null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotNull.class);
        verify(authorities, never()).findById(any());
    }

    @Test
    @DisplayName("Id nulo: não consulta o repositório")
    void rejectsNullId() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new UpdateAuthorityCommand(null, Authority.Type.MASTER)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotNull.class);
        verify(authorities, never()).findById(any(UUID.class));
    }

}
