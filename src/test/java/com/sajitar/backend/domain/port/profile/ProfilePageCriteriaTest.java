package com.sajitar.backend.domain.port.profile;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ProfilePageCriteria")
class ProfilePageCriteriaTest {

    private static final UUID VIEWER = UUID.fromString("550e8400-e29b-41d4-a716-446655440099");

    @Test
    @DisplayName("hasNameFilter e hasCursor cobrem nulo, em branco e preenchido")
    void nameFilterAndCursorFlags() {
        final var id = UUID.randomUUID();
        assertThat(criteria(null, null, null, true).hasNameFilter()).isFalse();
        assertThat(criteria("   ", null, null, true).hasNameFilter()).isFalse();
        assertThat(criteria("Silva", null, null, false).hasNameFilter()).isTrue();

        assertThat(criteria(null, null, id, true).hasCursor()).isFalse();
        assertThat(criteria(null, "   ", id, true).hasCursor()).isFalse();
        assertThat(criteria(null, "Maria Silva", null, true).hasCursor()).isFalse();
        assertThat(criteria(null, "Maria Silva", id, true).hasCursor()).isTrue();
    }

    @Test
    @DisplayName("withCursor preserva includeUnverified, includeReaders e viewerProfileId")
    void withCursorPreservesVisibility() {
        final var id = UUID.randomUUID();
        final var next = new ProfilePageCriteria("Silva", null, null, 10, false, false, false, VIEWER)
                .withCursor("Maria Silva", id, true);

        assertThat(next.includeUnverified()).isFalse();
        assertThat(next.includeReaders()).isFalse();
        assertThat(next.viewerProfileId()).isEqualTo(VIEWER);
        assertThat(next.reverse()).isTrue();
        assertThat(next.lastSeenName()).isEqualTo("Maria Silva");
        assertThat(next.lastSeenId()).isEqualTo(id);
        assertThat(next.nameContains()).isEqualTo("Silva");
    }

    private static ProfilePageCriteria criteria(
            final String nameContains,
            final String lastSeenName,
            final UUID lastSeenId,
            final boolean include) {
        return new ProfilePageCriteria(nameContains, lastSeenName, lastSeenId, 10, false, include, include, VIEWER);
    }

}
