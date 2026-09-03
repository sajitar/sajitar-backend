package com.sajitar.backend.adapter.in.web.contract.checker;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
    @DisplayName("página omite lastSeenType nulo na fábrica")
    void pageFromKeepsCursor() {
        final var checker = Checker.create(
                UUID.fromString("01989bad-6161-7000-0ae9-f440b10578ec"),
                Checker.Type.VERIFY_EMAIL);
        final var withoutCursor = CheckerPageResponse.from(100, null, List.of(checker));
        assertThat(withoutCursor.lastSeenType()).isNull();
        assertThat(withoutCursor.limit()).isEqualTo(100);
        assertThat(withoutCursor.content()).hasSize(1);

        final var withCursor = CheckerPageResponse.from(2, Checker.Type.CHANGE_EMAIL, List.of(checker));
        assertThat(withCursor.lastSeenType()).isEqualTo(Checker.Type.CHANGE_EMAIL);
    }

}
