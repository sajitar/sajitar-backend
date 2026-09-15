package com.sajitar.backend.domain.model.token;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Session")
class SessionTest {

    private static final UUID PROFILE_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    private static final UUID ACCESS_ID = UUID.fromString("018f3c2a-7b00-7c3d-9e1a-000000000001");

    private static final UUID REFRESH_ID = UUID.fromString("018f3c2a-7b00-7c3d-9e1a-000000000002");

    @Test
    @DisplayName("Abre a sessão com id UUIDv7 gerado no servidor e sem refresh")
    void opensWithGeneratedIdentifier() {
        final var session = Session.open(PROFILE_ID, ACCESS_ID, null);

        assertThat(session.id().version()).isEqualTo(7);
        assertThat(session.profileId()).isEqualTo(PROFILE_ID);
        assertThat(session.accessId()).isEqualTo(ACCESS_ID);
        assertThat(session.refreshId()).isNull();
    }

    @Test
    @DisplayName("O instante do login sai dos 48 bits de tempo do id")
    void readsLoginInstantFromIdentifier() {
        final var before = Instant.now().minusSeconds(1);

        final var session = Session.open(PROFILE_ID, ACCESS_ID, null);

        assertThat(session.bornAt()).isAfter(before).isBefore(Instant.now().plusSeconds(1));
    }

    @Test
    @DisplayName("withRefresh mantém o mesmo id da sessão")
    void keepsIdentifierWhenAttachingRefresh() {
        final var session = Session.open(PROFILE_ID, ACCESS_ID, null);

        final var withRefresh = session.withRefresh(REFRESH_ID);

        assertThat(withRefresh.id()).isEqualTo(session.id());
        assertThat(withRefresh.profileId()).isEqualTo(PROFILE_ID);
        assertThat(withRefresh.accessId()).isEqualTo(ACCESS_ID);
        assertThat(withRefresh.refreshId()).isEqualTo(REFRESH_ID);
    }

    @Test
    @DisplayName("Identidade é o id da sessão")
    void comparesByIdentifier() {
        final var session = Session.open(PROFILE_ID, ACCESS_ID, null);
        final var sameIdentity = new Session(session.id(), UUID.randomUUID(), UUID.randomUUID(), null);
        final var other = Session.open(PROFILE_ID, ACCESS_ID, null);

        assertThat(session)
                .isEqualTo(session)
                .isEqualTo(sameIdentity)
                .isNotEqualTo(other)
                .isNotEqualTo(PROFILE_ID)
                .hasSameHashCodeAs(sameIdentity);
    }

}
