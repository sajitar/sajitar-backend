package com.sajitar.backend.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.env.Environment;

@DisplayName("ActiveProfileConfiguration")
class ActiveProfileConfigurationTest {

    @ParameterizedTest
    @ValueSource(strings = { "LOCAL", "test" })
    @DisplayName("Aceita um perfil ativo explícito")
    void acceptsActiveProfile(final String profile) {
        final var environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[] { profile });

        assertThat(new ActiveProfileConfiguration(environment)).isNotNull();
    }

    @Test
    @DisplayName("Rejeita boot sem perfil ativo")
    void rejectsMissingActiveProfile() {
        final var environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[0]);

        final var thrown = catchThrowable(() -> new ActiveProfileConfiguration(environment));

        assertThat(thrown).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SPRING_PROFILES_ACTIVE");
    }

}
