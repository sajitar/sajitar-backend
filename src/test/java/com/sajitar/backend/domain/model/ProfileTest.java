package com.sajitar.backend.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Profile (agregado)")
class ProfileTest {

    @Test
    @DisplayName("withEmail e withPassword copiam os demais campos")
    void withersCopyRemainingFields() {
        final var original = Profile.create("Maria Silva", "desc", LocalDate.parse("1988-01-10"), "a@b.co", "12345678");

        final var byEmail = original.withEmail("c@d.co");
        assertThat(byEmail.id()).isEqualTo(original.id());
        assertThat(byEmail.email()).isEqualTo("c@d.co");
        assertThat(byEmail.name()).isEqualTo(original.name());

        final var byPassword = original.withPassword("outraSenha");
        assertThat(byPassword.id()).isEqualTo(original.id());
        assertThat(byPassword.password()).isEqualTo("outraSenha");
        assertThat(byPassword.email()).isEqualTo(original.email());
    }

    @Test
    @DisplayName("equals considera apenas o id e rejeita outros tipos")
    void equalsByIdOnly() {
        final var id = UUID.randomUUID();
        final var a = new Profile(id, "A", null, LocalDate.parse("1988-01-10"), "a@b.co", "12345678");
        final var b = new Profile(id, "B", "x", LocalDate.parse("1990-01-01"), "b@c.co", "87654321");
        final var c = a.withId(UUID.randomUUID());

        assertThat(a).isEqualTo(b).isNotEqualTo(c).isNotEqualTo("nao-e-perfil").isNotEqualTo(null);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

}
