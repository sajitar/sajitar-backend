package com.sajitar.backend.adapter.in.web.contract.checker;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sajitar.backend.application.page.Page;
import com.sajitar.backend.domain.model.checker.Checker;

@DisplayName("CheckerResponse")
class CheckerResponseTest {

    @Test
    @DisplayName("from copia a visão pública e requiredPayload")
    void fromCopiesPublicView() {
        final var checker = new Checker(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                UUID.fromString("01989bad-6161-7000-0ae9-f440b10578ec"),
                Checker.Type.CHANGE_EMAIL,
                "123456",
                null,
                10,
                3,
                Instant.parse("2001-04-24T21:00:00Z"));

        final var response = CheckerResponse.from(checker);

        assertThat(response.id()).isEqualTo(checker.id());
        assertThat(response.profileId()).isEqualTo(checker.profileId());
        assertThat(response.type()).isEqualTo(Checker.Type.CHANGE_EMAIL);
        assertThat(response.attempts()).isEqualTo(10);
        assertThat(response.replaces()).isEqualTo(3);
        assertThat(response.updatedAt()).isEqualTo(checker.updatedAt());
        assertThat(response.requiredPayload()).isTrue();
    }

    @Test
    @DisplayName("página copia content e metadados de Page")
    void pageFromCopiesPage() {
        final var checker = Checker.create(
                UUID.fromString("01989bad-6161-7000-0ae9-f440b10578ec"),
                Checker.Type.VERIFY_EMAIL);
        final var response = CheckerPageResponse.from(new Page<>(List.of(checker), 1, 2, false));
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().id()).isEqualTo(checker.id());
        assertThat(response.precedingElements()).isEqualTo(1);
        assertThat(response.followingElements()).isEqualTo(2);
        assertThat(response.reverse()).isFalse();
    }

}
