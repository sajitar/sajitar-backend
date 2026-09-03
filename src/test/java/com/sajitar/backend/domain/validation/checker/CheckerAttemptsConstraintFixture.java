package com.sajitar.backend.domain.validation.checker;

import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

final class CheckerAttemptsConstraintFixture {

    static Stream<Arguments> validArguments() {
        return Stream.of(
                Arguments.of(0, "Mínimo 0 deveria passar"),
                Arguments.of(5, "Valor intermediário deveria passar"),
                Arguments.of(10, "Máximo 10 deveria passar"));
    }

    static Stream<Arguments> outOfRangeArguments() {
        return Stream.of(
                Arguments.of(-1, "Abaixo do mínimo deveria falhar"),
                Arguments.of(11, "Acima do máximo deveria falhar"));
    }

    private CheckerAttemptsConstraintFixture() {
    }

}
