package com.sajitar.backend.domain.validation.checker;

import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

final class CheckerCodeConstraintFixture {

    static Stream<Arguments> validArguments() {
        return Stream.of(
                Arguments.of("123456", "Seis dígitos deveria passar"),
                Arguments.of("000000", "Zeros deveriam passar"),
                Arguments.of("999999", "Nove repetido deveria passar"));
    }

    static Stream<Arguments> blankArguments() {
        return Stream.of(
                Arguments.of(null, "Null deveria violar @NotBlank"),
                Arguments.of("", "Vazio deveria violar @NotBlank"),
                Arguments.of("   ", "Em branco deveria violar @NotBlank"));
    }

    static Stream<Arguments> invalidPatternArguments() {
        return Stream.of(
                Arguments.of("12345", "Cinco dígitos deveria violar @Pattern"),
                Arguments.of("1234567", "Sete dígitos deveria violar @Pattern"),
                Arguments.of("abcdef", "Letras deveria violar @Pattern"),
                Arguments.of("12345a", "Mistura deveria violar @Pattern"));
    }

    static Sample notBlankViolation() {
        return new Sample(
                "",
                "must not be blank",
                "code",
                "Vazio deveria gerar violação de @NotBlank",
                "A violação deveria vir de @NotBlank",
                "A mensagem deveria ser validation.not-blank",
                "O caminho deveria apontar para code");
    }

    static Sample patternViolation() {
        return new Sample(
                "12345",
                "must contain exactly 6 digits",
                "code",
                "Cinco dígitos deveria gerar violação de @Pattern",
                "A violação deveria vir de @Pattern",
                "A mensagem deveria ser validation.checker.code.pattern",
                "O caminho deveria apontar para code");
    }

    record Sample(
            String sampleInvalidValue,
            String expectedMessage,
            String expectedPropertyPath,
            String failureDescriptionViolationCount,
            String failureDescriptionConstraintAnnotation,
            String failureDescriptionMessage,
            String failureDescriptionPropertyPath) {
    }

    private CheckerCodeConstraintFixture() {
    }

}
