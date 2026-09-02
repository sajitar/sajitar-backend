package com.sajitar.backend.domain.validation;

import static com.sajitar.backend.domain.validation.Limit.Validation.validate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sajitar.backend.domain.validation.LimitConstraintFixture.LimitConstraintSample;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

/**
 * Testes da anotação {@link Limit}, organizados no estilo
 * <em>given</em> / <em>when</em> / <em>then</em>
 * (entrada e dados na fase <em>given</em>, execução de
 * {@link Limit.Validation#validate(Integer)} na fase <em>when</em>
 * e asserções na fase <em>then</em>).
 */
@DisplayName("Anotação @Limit")
public class LimitTest {

	@BeforeAll
	static void configureMax() {
		Limit.LimitValidator.configure(100);
	}

	@Nested
	@DisplayName("Valores aceitos")
	class AcceptedValues {

		@ParameterizedTest(name = "[{index}] {1}")
		@MethodSource("com.sajitar.backend.domain.validation.LimitConstraintFixture#validLimitArguments")
		void returnsInputWhenValid(final Integer limit, final String failureDescription) {
			// Given
			final var input = limit;
			final var description = failureDescription;

			// When
			final var result = whenLimitIsValidated(input);

			// Then
			assertThat(result).as(description).isEqualTo(input);
		}

		@Test
		@DisplayName("Mensagem de violação vem de @Limit quando o valor não é estritamente positivo")
		void exposesLimitConstraintDetailsWhenZero() {
			// Given
			final LimitConstraintSample sample = LimitConstraintFixture.positiveConstraintViolation();

			// When
			final var violations = whenLimitIsValidatedExpectingViolations(
					sample.sampleInvalidValue(),
					sample.failureDescriptionViolationCount());

			// Then
			thenSingleViolationMatchesConstraint(violations, sample, Limit.class);
		}

		@Test
		@DisplayName("Mensagem de violação vem de @Limit quando excede o máximo permitido")
		void exposesLimitConstraintDetailsWhenAboveCeiling() {
			// Given
			final LimitConstraintSample sample = LimitConstraintFixture.maxConstraintViolation();

			// When
			final var violations = whenLimitIsValidatedExpectingViolations(
					sample.sampleInvalidValue(),
					sample.failureDescriptionViolationCount());

			// Then
			thenSingleViolationMatchesConstraint(violations, sample, Limit.class);
		}
	}

	@Nested
	@DisplayName("Valores rejeitados (nulos)")
	class RejectedNullOrBlank {

		@ParameterizedTest(name = "[{index}] {1}")
		@MethodSource("com.sajitar.backend.domain.validation.LimitConstraintFixture#rejectedNullOrBlankArguments")
		void throwsWhenNull(final Integer limit, final String failureDescription) {
			// Given
			final var input = limit;
			final var description = failureDescription;

			// When
			final var violations = whenLimitIsValidatedExpectingViolations(input, description);

			// Then
			assertThat(violations).as(description).isNotEmpty();
			if (Objects.isNull(input)) {
				assertThat(annotationTypes(violations)).as(description).contains(jakarta.validation.constraints.NotNull.class);
			}
		}
	}

	@Nested
	@DisplayName("Valores rejeitados (não positivos)")
	class RejectedNotPositive {

		@ParameterizedTest(name = "[{index}] {1}")
		@MethodSource("com.sajitar.backend.domain.validation.LimitConstraintFixture#notPositiveArguments")
		void throwsWhenNotStrictlyPositive(final Integer limit, final String failureDescription) {
			// Given
			final var input = limit;
			final var description = failureDescription;

			// When
			final var violations = whenLimitIsValidatedExpectingViolations(input, description);

			// Then
			thenViolationsIncludePositiveConstraint(violations, description);
		}
	}

	@Nested
	@DisplayName("Valores rejeitados (acima do máximo)")
	class RejectedExceedsMax {

		@ParameterizedTest(name = "[{index}] {1}")
		@MethodSource("com.sajitar.backend.domain.validation.LimitConstraintFixture#exceedsMaxArguments")
		void throwsWhenAboveMax(final Integer limit, final String failureDescription) {
			// Given
			final var input = limit;
			final var description = failureDescription;

			// When
			final var violations = whenLimitIsValidatedExpectingViolations(input, description);

			// Then
			thenViolationsIncludeMaxConstraint(violations, description);
		}
	}

	private static Integer whenLimitIsValidated(final Integer input) {
		return validate(input);
	}

	private static Set<ConstraintViolation<?>> whenLimitIsValidatedExpectingViolations(final Integer input, final String assertionDescription) {
		final var thrown = catchThrowable(() -> validate(input));
		assertThat(thrown).as(assertionDescription).isInstanceOf(ConstraintViolationException.class);
		return ((ConstraintViolationException) thrown).getConstraintViolations();
	}

	private static void thenViolationsIncludePositiveConstraint(final Set<ConstraintViolation<?>> violations, final String failureDescription) {
		assertThat(violations).as(failureDescription).isNotEmpty();
		assertThat(annotationTypes(violations)).as(failureDescription).contains(Limit.class);
	}

	private static void thenViolationsIncludeMaxConstraint(final Set<ConstraintViolation<?>> violations, final String failureDescription) {
		assertThat(violations).as(failureDescription).isNotEmpty();
		assertThat(annotationTypes(violations)).as(failureDescription).contains(Limit.class);
	}

	private static void thenSingleViolationMatchesConstraint(
			final Set<ConstraintViolation<?>> violations,
			final LimitConstraintSample sample,
			final Class<?> constraintAnnotation) {
		assertThat(violations).as(sample.failureDescriptionViolationCount()).hasSize(1);
		final var violation = violations.iterator().next();
		assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType())
				.as(sample.failureDescriptionConstraintAnnotation())
				.isEqualTo(constraintAnnotation);
		assertThat(violation.getMessage())
				.as(sample.failureDescriptionMessage())
				.isEqualTo(sample.expectedMessagePtBr());
		assertThat(violation.getPropertyPath().toString())
				.as(sample.failureDescriptionPropertyPath())
				.isEqualTo(sample.expectedPropertyPath());
	}

	private static Set<Class<?>> annotationTypes(final Set<ConstraintViolation<?>> violations) {
		return violations.stream()
				.map(v -> v.getConstraintDescriptor().getAnnotation().annotationType())
				.collect(Collectors.toSet());
	}
}
