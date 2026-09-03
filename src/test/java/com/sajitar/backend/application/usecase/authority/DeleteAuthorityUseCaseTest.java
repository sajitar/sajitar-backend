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

import com.sajitar.backend.application.command.authority.DeleteAuthorityCommand;
import com.sajitar.backend.domain.exception.AuthorityNotFoundException;
import com.sajitar.backend.domain.port.authority.AuthorityRepository;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteAuthorityUseCase")
class DeleteAuthorityUseCaseTest {

    @Mock
    private AuthorityRepository authorities;

    private DeleteAuthorityUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteAuthorityUseCase(authorities, AuthorityUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Remove a authority")
    void deletesWhenPresent() {
        final var existing = AuthorityUseCaseFixture.persistedMaster();
        when(authorities.findById(existing.id())).thenReturn(Optional.of(existing));

        useCase.execute(new DeleteAuthorityCommand(existing.id()));

        verify(authorities).deleteById(existing.id());
    }

    @Test
    @DisplayName("Remove MEMBER")
    void deletesMember() {
        final var existing = AuthorityUseCaseFixture.persistedMember();
        when(authorities.findById(existing.id())).thenReturn(Optional.of(existing));

        useCase.execute(new DeleteAuthorityCommand(existing.id()));

        verify(authorities).deleteById(existing.id());
    }

    @Test
    @DisplayName("Lança AuthorityNotFoundException quando o id não existe")
    void throwsWhenMissing() {
        when(authorities.findById(AuthorityUseCaseFixture.ID)).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(new DeleteAuthorityCommand(AuthorityUseCaseFixture.ID)));

        assertThat(thrown).isInstanceOf(AuthorityNotFoundException.class);
        verify(authorities, never()).deleteById(any());
    }

    @Test
    @DisplayName("Id nulo: não consulta o repositório")
    void rejectsNullId() {
        final var thrown = catchThrowable(() -> useCase.execute(new DeleteAuthorityCommand(null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotNull.class);
        verify(authorities, never()).findById(any(UUID.class));
    }

}
