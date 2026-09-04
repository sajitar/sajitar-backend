package com.sajitar.backend.domain.validation.note;

import static com.sajitar.backend.domain.validation.note.Content.Validation.validate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sajitar.backend.domain.validation.note.ContentConstraintFixture.Sample;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@DisplayName("Anotação @Content")
class ContentTest {

    @Nested
    @DisplayName("Valores aceitos")
    class AcceptedValues {

        @ParameterizedTest(name = "[{index}] {1}")
        @MethodSource("com.sajitar.backend.domain.validation.note.ContentConstraintFixture#validArguments")
        void returnsInputWhenValid(final String content, final String failureDescription) {
            final var result = validate(content);
            assertThat(result).as(failureDescription).isEqualTo(content);
        }

        @Test
        @DisplayName("Mensagem de violação vem do @NotBlank quando o valor é vazio")
        void exposesNotBlankWhenEmpty() {
            final Sample sample = ContentConstraintFixture.notBlankViolation();
            final var violations = expectViolations(sample.sampleInvalidValue(), sample.failureDescriptionViolationCount());
            assertThat(violations).as(sample.failureDescriptionViolationCount()).isNotEmpty();
            assertThat(annotationTypes(violations)).as(sample.failureDescriptionConstraintAnnotation()).contains(NotBlank.class);
            assertThat(violations.stream().filter(v -> v.getConstraintDescriptor().getAnnotation().annotationType().equals(NotBlank.class))
                    .findFirst().orElseThrow().getMessage())
                    .as(sample.failureDescriptionMessage())
                    .isEqualTo(sample.expectedMessage());
        }

        @Test
        @DisplayName("Mensagem de violação vem do @Size quando o texto é longo demais")
        void exposesSizeWhenTooLong() {
            final Sample sample = ContentConstraintFixture.sizeViolation();
            final var violations = expectViolations(sample.sampleInvalidValue(), sample.failureDescriptionViolationCount());
            thenSingleSize(violations, sample);
        }
    }

    @Nested
    @DisplayName("Valores rejeitados (em branco)")
    class RejectedBlank {

        @ParameterizedTest(name = "[{index}] {1}")
        @MethodSource("com.sajitar.backend.domain.validation.note.ContentConstraintFixture#blankArguments")
        void throwsWhenBlank(final String content, final String failureDescription) {
            final var violations = expectViolations(content, failureDescription);
            assertThat(violations).as(failureDescription).isNotEmpty();
            assertThat(annotationTypes(violations)).as(failureDescription).contains(NotBlank.class);
        }
    }

    @Nested
    @DisplayName("Valores rejeitados (tamanho)")
    class RejectedSize {

        @ParameterizedTest(name = "[{index}] {1}")
        @MethodSource("com.sajitar.backend.domain.validation.note.ContentConstraintFixture#tooLongArguments")
        void throwsWhenTooLong(final String content, final String failureDescription) {
            final var violations = expectViolations(content, failureDescription);
            assertThat(violations).as(failureDescription).isNotEmpty();
            assertThat(annotationTypes(violations)).as(failureDescription).contains(Size.class);
        }
    }

    @Test
    @DisplayName("validate(validator, value) aceita texto válido")
    void validateWithValidatorAcceptsValid() {
        try (final var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            assertThat(Content.Validation.validate(factory.getValidator(), "Uma nota.")).isEqualTo("Uma nota.");
        }
    }

    private static Set<ConstraintViolation<?>> expectViolations(final String input, final String assertionDescription) {
        final var thrown = catchThrowable(() -> validate(input));
        assertThat(thrown).as(assertionDescription).isInstanceOf(ConstraintViolationException.class);
        return ((ConstraintViolationException) thrown).getConstraintViolations();
    }

    private static void thenSingleSize(final Set<ConstraintViolation<?>> violations, final Sample sample) {
        final var size = violations.stream()
                .filter(v -> v.getConstraintDescriptor().getAnnotation().annotationType().equals(Size.class))
                .findFirst()
                .orElseThrow();
        assertThat(size.getMessage()).as(sample.failureDescriptionMessage()).isEqualTo(sample.expectedMessage());
        assertThat(size.getPropertyPath().toString()).as(sample.failureDescriptionPropertyPath())
                .isEqualTo(sample.expectedPropertyPath());
    }

    private static Set<Class<?>> annotationTypes(final Set<ConstraintViolation<?>> violations) {
        return violations.stream()
                .map(v -> v.getConstraintDescriptor().getAnnotation().annotationType())
                .collect(Collectors.toSet());
    }

}
