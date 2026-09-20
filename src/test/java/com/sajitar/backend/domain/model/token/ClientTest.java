package com.sajitar.backend.domain.model.token;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Client")
class ClientTest {

    @ParameterizedTest(name = "{0} vale {1}")
    @CsvSource({
            "DESKTOP, desktop",
            "MOBILE, mobile",
            "TABLET, tablet",
            "UNKNOWN, unknown"
    })
    @DisplayName("Cada dispositivo tem o valor gravado no Redis e devolvido no JSON")
    void exposesDeviceValue(final Client.Device device, final String value) {
        assertThat(device.value()).isEqualTo(value);
        assertThat(Client.Device.of(value)).isEqualTo(device);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   ", "tv" })
    @DisplayName("Valor ausente ou desconhecido vira UNKNOWN")
    void unknownWhenRawIsBlankOrUnrecognized(final String raw) {
        assertThat(Client.Device.of(raw)).isEqualTo(Client.Device.UNKNOWN);
    }

    @Test
    @DisplayName("Guarda name, os e device")
    void holdsParsedFields() {
        final var client = new Client("Chrome", "Linux", Client.Device.DESKTOP);

        assertThat(client.name()).isEqualTo("Chrome");
        assertThat(client.os()).isEqualTo("Linux");
        assertThat(client.device()).isEqualTo(Client.Device.DESKTOP);
    }

}
