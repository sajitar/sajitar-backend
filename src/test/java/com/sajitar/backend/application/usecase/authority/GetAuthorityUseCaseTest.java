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

import com.sajitar.backend.domain.port.authority.AuthorityRepository;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetAuthorityUseCase")
class GetAuthorityUseCaseTest {

    @Mock
    private AuthorityRepository authorities;

    private GetAuthorityUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetAuthorityUseCase(authorities, AuthorityUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Delega ao repositório quando o id é válido")
    void delegatesWhenIdIsValid() {
        when(authorities.findById(AuthorityUseCaseFixture.ID)).thenReturn(Optional.empty());

        assertThat(useCase.execute(AuthorityUseCaseFixture.ID)).isEmpty();
        verify(authorities).findById(AuthorityUseCaseFixture.ID);
    }

    @Test
    @DisplayName("Retorna a authority quando encontrada")
    void returnsWhenFound() {
        final var existing = AuthorityUseCaseFixture.persistedMaster();
        when(authorities.findById(existing.id())).thenReturn(Optional.of(existing));

        assertThat(useCase.execute(existing.id())).contains(existing);
    }

    @Test
    @DisplayName("Id nulo: não chama o repositório")
    void rejectsNullId() {
        final var thrown = catchThrowable(() -> useCase.execute(null));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotNull.class);
        verify(authorities, never()).findById(any(UUID.class));
    }

}
