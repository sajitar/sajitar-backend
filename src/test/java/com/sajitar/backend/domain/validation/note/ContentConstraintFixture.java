package com.sajitar.backend.domain.validation.note;

import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

final class ContentConstraintFixture {

    static Stream<Arguments> validArguments() {
        return Stream.of(
                Arguments.of("x", "Um caractere deveria passar"),
                Arguments.of("Uma nota pública.", "Texto curto deveria passar"),
                Arguments.of("a".repeat(Content.MAX_SIZE), "Mil caracteres deveriam passar"));
    }

    static Stream<Arguments> blankArguments() {
        return Stream.of(
                Arguments.of(null, "Null deveria violar @NotBlank"),
                Arguments.of("", "Vazio deveria violar @NotBlank"),
                Arguments.of("   ", "Em branco deveria violar @NotBlank"));
    }

    static Stream<Arguments> tooLongArguments() {
        return Stream.of(
                Arguments.of("a".repeat(Content.MAX_SIZE + 1), "Mil e um caracteres deveriam violar @Size"));
    }

    static Sample notBlankViolation() {
        return new Sample(
                "",
                "must not be blank",
                "content",
                "Vazio deveria gerar violação de @NotBlank",
                "A violação deveria vir de @NotBlank",
                "A mensagem deveria ser validation.not-blank",
                "O caminho deveria apontar para content");
    }

    static Sample sizeViolation() {
        return new Sample(
                "a".repeat(Content.MAX_SIZE + 1),
                "must contain at most " + Content.MAX_SIZE + " characters",
                "content",
                "Texto longo deveria gerar violação de @Size",
                "A violação deveria vir de @Size",
                "A mensagem deveria ser validation.size.max",
                "O caminho deveria apontar para content");
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

    private ContentConstraintFixture() {
    }

}
