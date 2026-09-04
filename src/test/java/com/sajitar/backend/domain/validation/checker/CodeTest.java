package com.sajitar.backend.domain.validation.checker;

import static com.sajitar.backend.domain.validation.checker.Code.Validation.validate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sajitar.backend.domain.validation.checker.CodeConstraintFixture.Sample;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@DisplayName("Anotação @Code")
class CodeTest {

    @Nested
    @DisplayName("Valores aceitos")
    class AcceptedValues {

        @ParameterizedTest(name = "[{index}] {1}")
        @MethodSource("com.sajitar.backend.domain.validation.checker.CodeConstraintFixture#validArguments")
        void returnsInputWhenValid(final String code, final String failureDescription) {
            final var result = validate(code);
            assertThat(result).as(failureDescription).isEqualTo(code);
        }

        @Test
        @DisplayName("Mensagem de violação vem do @NotBlank quando o valor é vazio")
        void exposesNotBlankWhenEmpty() {
            final Sample sample = CodeConstraintFixture.notBlankViolation();
            final var violations = expectViolations(sample.sampleInvalidValue(), sample.failureDescriptionViolationCount());
            assertThat(violations).as(sample.failureDescriptionViolationCount()).isNotEmpty();
            assertThat(annotationTypes(violations)).as(sample.failureDescriptionConstraintAnnotation()).contains(NotBlank.class);
            assertThat(violations.stream().filter(v -> v.getConstraintDescriptor().getAnnotation().annotationType().equals(NotBlank.class))
                    .findFirst().orElseThrow().getMessage())
                    .as(sample.failureDescriptionMessage())
                    .isEqualTo(sample.expectedMessage());
        }

        @Test
        @DisplayName("Mensagem de violação vem do @Pattern quando o formato é inválido")
        void exposesPatternWhenInvalid() {
            final Sample sample = CodeConstraintFixture.patternViolation();
            final var violations = expectViolations(sample.sampleInvalidValue(), sample.failureDescriptionViolationCount());
            thenSinglePattern(violations, sample);
        }
    }

    @Nested
    @DisplayName("Valores rejeitados (em branco)")
    class RejectedBlank {

        @ParameterizedTest(name = "[{index}] {1}")
        @MethodSource("com.sajitar.backend.domain.validation.checker.CodeConstraintFixture#blankArguments")
        void throwsWhenBlank(final String code, final String failureDescription) {
            final var violations = expectViolations(code, failureDescription);
            assertThat(violations).as(failureDescription).isNotEmpty();
            assertThat(annotationTypes(violations)).as(failureDescription).contains(NotBlank.class);
        }
    }

    @Test
    @DisplayName("validate(validator, value) aceita código válido")
    void validateWithValidatorAcceptsValid() {
        try (final var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            assertThat(Code.Validation.validate(factory.getValidator(), "123456")).isEqualTo("123456");
        }
    }

    @Nested
    @DisplayName("Valores rejeitados (padrão)")
    class RejectedPattern {

        @ParameterizedTest(name = "[{index}] {1}")
        @MethodSource("com.sajitar.backend.domain.validation.checker.CodeConstraintFixture#invalidPatternArguments")
        void throwsWhenPatternInvalid(final String code, final String failureDescription) {
            final var violations = expectViolations(code, failureDescription);
            assertThat(violations).as(failureDescription).isNotEmpty();
            assertThat(annotationTypes(violations)).as(failureDescription).contains(Pattern.class);
        }
    }

    private static Set<ConstraintViolation<?>> expectViolations(final String input, final String assertionDescription) {
        final var thrown = catchThrowable(() -> validate(input));
        assertThat(thrown).as(assertionDescription).isInstanceOf(ConstraintViolationException.class);
        return ((ConstraintViolationException) thrown).getConstraintViolations();
    }

    private static void thenSinglePattern(final Set<ConstraintViolation<?>> violations, final Sample sample) {
        final var pattern = violations.stream()
                .filter(v -> v.getConstraintDescriptor().getAnnotation().annotationType().equals(Pattern.class))
                .findFirst()
                .orElseThrow();
        assertThat(pattern.getMessage()).as(sample.failureDescriptionMessage()).isEqualTo(sample.expectedMessage());
        assertThat(pattern.getPropertyPath().toString()).as(sample.failureDescriptionPropertyPath())
                .isEqualTo(sample.expectedPropertyPath());
    }

    private static Set<Class<?>> annotationTypes(final Set<ConstraintViolation<?>> violations) {
        return violations.stream()
                .map(v -> v.getConstraintDescriptor().getAnnotation().annotationType())
                .collect(Collectors.toSet());
    }

}
