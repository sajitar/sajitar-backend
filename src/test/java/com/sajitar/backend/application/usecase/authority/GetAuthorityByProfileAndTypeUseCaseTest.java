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

import com.sajitar.backend.application.query.authority.GetAuthorityByProfileAndTypeQuery;
import com.sajitar.backend.domain.model.authority.Authority;
import com.sajitar.backend.domain.port.authority.AuthorityRepository;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetAuthorityByProfileAndTypeUseCase")
class GetAuthorityByProfileAndTypeUseCaseTest {

    @Mock
    private AuthorityRepository authorities;

    private GetAuthorityByProfileAndTypeUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetAuthorityByProfileAndTypeUseCase(authorities, AuthorityUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Retorna a authority do par quando existe")
    void returnsWhenFound() {
        final var existing = AuthorityUseCaseFixture.persistedMaster();
        when(authorities.findByProfileIdAndType(existing.profileId(), existing.type()))
                .thenReturn(Optional.of(existing));

        final var result = useCase.execute(new GetAuthorityByProfileAndTypeQuery(existing.profileId(), existing.type()));

        assertThat(result).contains(existing);
    }

    @Test
    @DisplayName("Retorna vazio quando o par não existe")
    void returnsEmptyWhenMissing() {
        when(authorities.findByProfileIdAndType(AuthorityUseCaseFixture.PROFILE_ID, Authority.Type.READER))
                .thenReturn(Optional.empty());

        final var result = useCase.execute(new GetAuthorityByProfileAndTypeQuery(
                AuthorityUseCaseFixture.PROFILE_ID, Authority.Type.READER));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("profileId nulo: não consulta o repositório")
    void rejectsNullProfileId() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new GetAuthorityByProfileAndTypeQuery(null, Authority.Type.MASTER)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotNull.class);
        verify(authorities, never()).findByProfileIdAndType(any(), any());
    }

    @Test
    @DisplayName("type nulo: não consulta o repositório")
    void rejectsNullType() {
        final var thrown = catchThrowable(
                () -> useCase.execute(new GetAuthorityByProfileAndTypeQuery(AuthorityUseCaseFixture.PROFILE_ID, null)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(authorities, never()).findByProfileIdAndType(any(UUID.class), any());
    }

}
