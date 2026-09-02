package com.sajitar.backend.application.page;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Page")
class PageTest {

    @Test
    @DisplayName("content nulo vira lista vazia")
    void nullContentBecomesEmptyList() {
        final var page = new Page<String>(null, 1, 2, true);
        assertThat(page.content()).isEmpty();
        assertThat(page.isEmpty()).isTrue();
        assertThat(page.precedingElements()).isEqualTo(1);
        assertThat(page.followingElements()).isEqualTo(2);
        assertThat(page.reverse()).isTrue();
    }

}
