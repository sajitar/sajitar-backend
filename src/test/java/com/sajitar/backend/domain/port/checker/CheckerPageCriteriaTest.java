package com.sajitar.backend.domain.port.checker;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sajitar.backend.domain.model.checker.Checker;

@DisplayName("CheckerPageCriteria")
class CheckerPageCriteriaTest {

    @Test
    @DisplayName("hasCursor é verdadeiro somente quando lastSeenType está preenchido")
    void hasCursorWhenLastSeenTypeIsPresent() {
        final var profileId = UUID.randomUUID();
        assertThat(new CheckerPageCriteria(profileId, null, 10, false).hasCursor()).isFalse();
        assertThat(new CheckerPageCriteria(profileId, Checker.Type.CHANGE_EMAIL, 10, false).hasCursor()).isTrue();
    }

    @Test
    @DisplayName("withCursor preserva profileId e limit e troca tipo e reverse")
    void withCursorReplacesTypeAndReverse() {
        final var profileId = UUID.randomUUID();
        final var original = new CheckerPageCriteria(profileId, Checker.Type.CHANGE_EMAIL, 5, false);
        final var next = original.withCursor(Checker.Type.VERIFY_EMAIL, true);
        assertThat(next.profileId()).isEqualTo(profileId);
        assertThat(next.limit()).isEqualTo(5);
        assertThat(next.lastSeenType()).isEqualTo(Checker.Type.VERIFY_EMAIL);
        assertThat(next.reverse()).isTrue();
    }

}
