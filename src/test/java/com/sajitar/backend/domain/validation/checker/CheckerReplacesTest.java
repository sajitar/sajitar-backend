package com.sajitar.backend.domain.validation.checker;

import static com.sajitar.backend.domain.validation.checker.CheckerReplaces.Validation.validate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@DisplayName("Anotação @CheckerReplaces")
class CheckerReplacesTest {

    @Nested
    @DisplayName("Valores aceitos")
    class AcceptedValues {

        @ParameterizedTest(name = "[{index}] {1}")
        @MethodSource("com.sajitar.backend.domain.validation.checker.CheckerReplacesConstraintFixture#validArguments")
        void returnsInputWhenValid(final Integer replaces, final String failureDescription) {
            assertThat(validate(replaces)).as(failureDescription).isEqualTo(replaces);
        }
    }

    @Nested
    @DisplayName("Valores rejeitados")
    class RejectedValues {

        @Test
        @DisplayName("Null viola @NotNull")
        void rejectsNull() {
            final var violations = expectViolations(null, "Null deveria falhar");
            assertThat(annotationTypes(violations)).contains(NotNull.class);
        }

        @ParameterizedTest(name = "[{index}] {1}")
        @MethodSource("com.sajitar.backend.domain.validation.checker.CheckerReplacesConstraintFixture#outOfRangeArguments")
        void rejectsOutOfRange(final Integer replaces, final String failureDescription) {
            final var violations = expectViolations(replaces, failureDescription);
            assertThat(violations).as(failureDescription).isNotEmpty();
            assertThat(annotationTypes(violations)).as(failureDescription).containsAnyOf(Min.class, Max.class);
        }
    }

    @Test
    @DisplayName("validate(validator, value) aceita valor válido")
    void validateWithValidatorAcceptsValid() {
        try (final var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            assertThat(CheckerReplaces.Validation.validate(factory.getValidator(), 1)).isEqualTo(1);
        }
    }

    private static Set<ConstraintViolation<?>> expectViolations(final Integer input, final String assertionDescription) {
        final var thrown = catchThrowable(() -> validate(input));
        assertThat(thrown).as(assertionDescription).isInstanceOf(ConstraintViolationException.class);
        return ((ConstraintViolationException) thrown).getConstraintViolations();
    }

    private static Set<Class<?>> annotationTypes(final Set<ConstraintViolation<?>> violations) {
        return violations.stream()
                .map(v -> v.getConstraintDescriptor().getAnnotation().annotationType())
                .collect(Collectors.toSet());
    }

}
