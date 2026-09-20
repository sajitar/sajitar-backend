package com.sajitar.backend.domain.model.token;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ActiveSession")
class ActiveSessionTest {

    @Test
    @DisplayName("Guarda o id da sessão e o client parseado")
    void holdsIdentifierAndClient() {
        final var id = UUID.fromString("018f3c2a-7b00-7c3d-9e1a-000000000010");
        final var client = new Client("Safari", "iOS", Client.Device.MOBILE);

        final var session = new ActiveSession(id, client);

        assertThat(session.id()).isEqualTo(id);
        assertThat(session.client()).isEqualTo(client);
    }

    @Test
    @DisplayName("Aceita sessão sem client")
    void allowsMissingClient() {
        final var id = UUID.fromString("018f3c2a-7b00-7c3d-9e1a-000000000020");

        assertThat(new ActiveSession(id, null).client()).isNull();
    }

}
