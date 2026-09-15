package com.sajitar.backend.domain.model.token;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("TokenUse")
class TokenUseTest {

    @ParameterizedTest(name = "{0} vale {1} na claim token_use")
    @CsvSource({
            "ACCESS, access",
            "REFRESH, refresh"
    })
    @DisplayName("Cada uso tem o valor que vai na claim token_use")
    void exposesClaimValue(final TokenUse use, final String value) {
        assertThat(use.value()).isEqualTo(value);
    }

}
