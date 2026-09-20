package com.sajitar.backend.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("MailProperties")
class MailPropertiesTest {

    @Test
    @DisplayName("Aceita host, porta SMTP e remetente preenchidos")
    void acceptsValidConfiguration() {
        final var properties = MailPropertiesFixture.defaults();

        assertThat(properties.host()).isEqualTo(MailPropertiesFixture.HOST);
        assertThat(properties.port()).isEqualTo(MailPropertiesFixture.PORT);
        assertThat(properties.from()).isEqualTo(MailPropertiesFixture.FROM);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   " })
    @DisplayName("Rejeita host em branco")
    void rejectsBlankHost(final String host) {
        final var thrown = catchThrowable(() -> new MailProperties(host, MailPropertiesFixture.PORT,
                MailPropertiesFixture.FROM));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("host");
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -1, 65536 })
    @DisplayName("Rejeita porta fora de 1..65535")
    void rejectsPortOutOfRange(final int port) {
        final var thrown = catchThrowable(() -> new MailProperties(MailPropertiesFixture.HOST, port,
                MailPropertiesFixture.FROM));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("port");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   " })
    @DisplayName("Rejeita remetente em branco")
    void rejectsBlankFrom(final String from) {
        final var thrown = catchThrowable(() -> new MailProperties(MailPropertiesFixture.HOST,
                MailPropertiesFixture.PORT, from));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("from");
    }

}
