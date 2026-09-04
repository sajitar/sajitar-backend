package com.sajitar.backend.domain.validation.checker;

import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

final class ReplacesConstraintFixture {

    static Stream<Arguments> validArguments() {
        return Stream.of(
                Arguments.of(0, "Mínimo 0 deveria passar"),
                Arguments.of(2, "Valor intermediário deveria passar"),
                Arguments.of(3, "Máximo 3 deveria passar"));
    }

    static Stream<Arguments> outOfRangeArguments() {
        return Stream.of(
                Arguments.of(-1, "Abaixo do mínimo deveria falhar"),
                Arguments.of(4, "Acima do máximo deveria falhar"));
    }

    private ReplacesConstraintFixture() {
    }

}
