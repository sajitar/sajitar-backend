package com.sajitar.backend.domain.model.token;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("AttemptScope")
class AttemptScopeTest {

    @ParameterizedTest(name = "{0} vale {1} na chave Redis")
    @CsvSource({
            "CREDENTIALS, credentials",
            "REFRESH, refresh"
    })
    @DisplayName("Cada escopo tem o valor que entra no prefixo attempt:")
    void exposesValue(final AttemptScope scope, final String value) {
        assertThat(scope.value()).isEqualTo(value);
    }

}
