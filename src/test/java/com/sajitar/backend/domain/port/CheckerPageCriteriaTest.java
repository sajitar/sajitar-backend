package com.sajitar.backend.domain.port;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sajitar.backend.domain.model.Checker;

@DisplayName("CheckerPageCriteria")
class CheckerPageCriteriaTest {

    @Test
    @DisplayName("hasCursor é verdadeiro somente quando lastSeenType está preenchido")
    void hasCursorWhenLastSeenTypeIsPresent() {
        final var profileId = UUID.randomUUID();
        assertThat(new CheckerPageCriteria(profileId, null, 10).hasCursor()).isFalse();
        assertThat(new CheckerPageCriteria(profileId, Checker.Type.CHANGE_EMAIL, 10).hasCursor()).isTrue();
    }

}
