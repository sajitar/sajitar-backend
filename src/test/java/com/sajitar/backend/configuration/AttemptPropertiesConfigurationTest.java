package com.sajitar.backend.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@DisplayName("AttemptPropertiesConfiguration")
class AttemptPropertiesConfigurationTest {

    @Test
    @DisplayName("Registra AttemptProperties no contexto")
    void enablesAttemptProperties() {
        assertThat(new AttemptPropertiesConfiguration()).isNotNull();
        assertThat(AttemptPropertiesConfiguration.class.getAnnotation(EnableConfigurationProperties.class).value())
                .containsExactly(AttemptProperties.class);
    }

}
