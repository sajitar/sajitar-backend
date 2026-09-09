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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sajitar.backend.application.command.authority.PatchAuthorityCommand;
import com.sajitar.backend.domain.exception.AuthorityNotFoundException;
import com.sajitar.backend.domain.exception.AuthorityTypeAlreadyExistsException;
import com.sajitar.backend.domain.model.authority.Authority;
import com.sajitar.backend.domain.port.authority.AuthorityRepository;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;

@ExtendWith(MockitoExtension.class)
@DisplayName("PatchAuthorityUseCase")
class PatchAuthorityUseCaseTest {

    @Mock
    private AuthorityRepository authorities;

    private PatchAuthorityUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new PatchAuthorityUseCase(authorities, AuthorityUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Atualiza o type quando o destino está livre")
    void patchesType() {
        final var existing = AuthorityUseCaseFixture.persistedMaster();
        when(authorities.findById(existing.id())).thenReturn(Optional.of(existing));
        when(authorities.findByProfileIdAndType(existing.profileId(), Authority.Type.MEMBER))
                .thenReturn(Optional.empty());
        when(authorities.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(new PatchAuthorityCommand(existing.id(), Authority.Type.MEMBER));

        assertThat(saved.id()).isEqualTo(existing.id());
        assertThat(saved.profileId()).isEqualTo(existing.profileId());
        assertThat(saved.type()).isEqualTo(Authority.Type.MEMBER);
    }

    @Test
    @DisplayName("Type igual ao persistido não grava")
    void sameTypeDoesNotSave() {
        final var existing = AuthorityUseCaseFixture.persistedMaster();
        when(authorities.findById(existing.id())).thenReturn(Optional.of(existing));

        final var saved = useCase.execute(new PatchAuthorityCommand(existing.id(), existing.type()));

        assertThat(saved).isEqualTo(existing);
        verify(authorities, never()).save(any());
        verify(authorities, never()).findByProfileIdAndType(any(), any());
    }

    @Test
    @DisplayName("Body vazio não persiste e devolve o estado atual")
    void emptyPatchDoesNotSave() {
        final var existing = AuthorityUseCaseFixture.persistedMaster();
        when(authorities.findById(existing.id())).thenReturn(Optional.of(existing));

        final var saved = useCase.execute(AuthorityUseCaseFixture.emptyPatchCommand());

        assertThat(saved).isEqualTo(existing);
        verify(authorities, never()).save(any());
    }

    @Test
    @DisplayName("Troca para tipo já existente no perfil lança 409")
    void rejectsDuplicateType() {
        final var existing = AuthorityUseCaseFixture.persistedMaster();
        when(authorities.findById(existing.id())).thenReturn(Optional.of(existing));
        when(authorities.findByProfileIdAndType(existing.profileId(), Authority.Type.READER))
                .thenReturn(Optional.of(AuthorityUseCaseFixture.persistedReader()));

        final var thrown = catchThrowable(() -> useCase.execute(
                new PatchAuthorityCommand(existing.id(), Authority.Type.READER)));

        assertThat(thrown).isInstanceOf(AuthorityTypeAlreadyExistsException.class);
        verify(authorities, never()).save(any());
    }

    @Test
    @DisplayName("Lança AuthorityNotFoundException quando o id não existe")
    void throwsWhenMissing() {
        when(authorities.findById(AuthorityUseCaseFixture.ID)).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(AuthorityUseCaseFixture.emptyPatchCommand()));

        assertThat(thrown).isInstanceOf(AuthorityNotFoundException.class);
        verify(authorities, never()).save(any());
    }

    @Test
    @DisplayName("Id nulo: não consulta o repositório")
    void rejectsNullId() {
        final var thrown = catchThrowable(() -> useCase.execute(new PatchAuthorityCommand(null, null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotNull.class);
        verify(authorities, never()).findById(any(UUID.class));
    }

    @Test
    @DisplayName("hasChanges cobre type presente ou ausente")
    void hasChangesSemantics() {
        assertThat(AuthorityUseCaseFixture.emptyPatchCommand().hasChanges()).isFalse();
        assertThat(new PatchAuthorityCommand(AuthorityUseCaseFixture.ID, Authority.Type.MASTER).hasChanges())
                .isTrue();
    }

}
