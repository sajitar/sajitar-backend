package com.sajitar.backend.domain.validation;

import static com.sajitar.backend.domain.validation.Year.Validation.validate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

/**
 * Testes da anotação {@link Year}, do validador {@link Year.YearValidation} e da
 * fábrica {@link Year.Validation#validate(LocalDate)}, no estilo
 * <em>given</em> / <em>when</em> / <em>then</em> alinhado a
 * {@link com.sajitar.backend.domain.validation.profile.EmailTest}.
 */
@SpringBootTest
@DisplayName("Anotação @Year (domínio)")
public class YearTest {

	@Nested
	@DisplayName("Year.Validation.validate")
	class ValidationFactory {

		@ParameterizedTest(name = "[{index}] {1}")
		@MethodSource("com.sajitar.backend.domain.validation.YearConstraintFixture#validationAcceptedArguments")
		void returnsInputWhenValid(final LocalDate input, final String failureDescription) {
			// Given
			final var description = failureDescription;

			// When
			final var result = whenYearIsValidated(input);

			// Then
			assertThat(result).as(description).isEqualTo(input);
		}

		@ParameterizedTest(name = "[{index}] {1}")
		@MethodSource("com.sajitar.backend.domain.validation.YearConstraintFixture#validationRejectedArguments")
		void throwsConstraintViolationWhenOutOfRange(final LocalDate input, final String failureDescription) {
			// Given
			final var description = failureDescription;

			// When
			final var violations = whenYearIsValidatedExpectingViolations(input, description);

			// Then
			thenViolationsIncludeYearConstraint(violations, description);
			thenSingleViolationTargetsYearProperty(violations, description);
		}

		@Test
		@DisplayName("Exceção carrega exatamente uma violação do constraint @Year")
		void exposesYearConstraintDescriptorOnViolation() {
			// Given
			final var input = YearConstraintFixture.today().minusYears(200);

			// When
			final var violations = whenYearIsValidatedExpectingViolations(
					input,
					YearConstraintFixture.describeViolationSample(input, "idade fora de [0, 150]"));

			// Then
			assertThat(violations).hasSize(1);
			final var violation = violations.iterator().next();
			assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType())
					.isEqualTo(Year.class);
			assertThat(violation.getInvalidValue()).isEqualTo(input);
		}
	}

	@Nested
	@DisplayName("Year.YearValidation (unitário, após initialize)")
	class YearValidationUnit {

		@ParameterizedTest(name = "[{index}] {1}")
		@MethodSource("com.sajitar.backend.domain.validation.YearConstraintFixture#yearValidationAdultStrictAcceptedArguments")
		void acceptsDatesInsideWindow(final LocalDate target, final String failureDescription) {
			// Given
			final var validator = new Year.YearValidation();
			validator.initialize(YearConstraintFixture.yearAnnotation(YearConstraintFixture.AdultStrict.class, "birthDate"));

			// When
			final var valid = validator.isValid(target, null);

			// Then
			assertThat(valid).as(failureDescription).isTrue();
		}

		@ParameterizedTest(name = "[{index}] {1}")
		@MethodSource("com.sajitar.backend.domain.validation.YearConstraintFixture#yearValidationAdultStrictRejectedArguments")
		void rejectsDatesOutsideWindowOrNull(final LocalDate target, final String failureDescription) {
			// Given
			final var validator = new Year.YearValidation();
			validator.initialize(YearConstraintFixture.yearAnnotation(YearConstraintFixture.AdultStrict.class, "birthDate"));

			// When
			final var valid = validator.isValid(target, null);

			// Then
			assertThat(valid).as(failureDescription).isFalse();
		}

		@ParameterizedTest(name = "[{index}] {2}")
		@MethodSource("com.sajitar.backend.domain.validation.YearConstraintFixture#yearValidationShortWindowBoundaryArguments")
		void honorsInclusiveMinMaxAndNullability(
				final Year meta,
				final LocalDate target,
				final boolean expectedValid,
				final String failureDescription) {
			// Given
			final var validator = new Year.YearValidation();
			validator.initialize(meta);

			// When
			final var valid = validator.isValid(target, null);

			// Then
			assertThat(valid).as(failureDescription).isEqualTo(expectedValid);
		}

		@ParameterizedTest(name = "[{index}] {3}")
		@MethodSource("com.sajitar.backend.domain.validation.YearConstraintFixture#yearValidationNonNullDefaultRangeArguments")
		void respectsNullableWithDefaultNumericBounds(
				final Year meta,
				final LocalDate target,
				final boolean expectedValid,
				final String failureDescription) {
			// Given
			final var validator = new Year.YearValidation();
			validator.initialize(meta);

			// When
			final var valid = validator.isValid(target, null);

			// Then
			assertThat(valid).as(failureDescription).isEqualTo(expectedValid);
		}

		@ParameterizedTest(name = "[{index}] {3}")
		@MethodSource("com.sajitar.backend.domain.validation.YearConstraintFixture#yearValidationDefaultMetaArguments")
		void defaultAnnotationKeepsPermissiveNumericWindow(
				final Year meta,
				final LocalDate target,
				final boolean expectedValid,
				final String failureDescription) {
			// Given
			final var validator = new Year.YearValidation();
			validator.initialize(meta);

			// When
			final var valid = validator.isValid(target, null);

			// Then
			assertThat(valid).as(failureDescription).isEqualTo(expectedValid);
		}

		@Test
		@DisplayName("isValid usa apenas Period.getYears(): amanhã ainda pode contar 0 anos e passar min = 0")
		void subYearOffsetFutureMayPassMinZero() {
			// Given
			final var validator = new Year.YearValidation();
			validator.initialize(YearConstraintFixture.yearAnnotation(YearConstraintFixture.DefaultYearMeta.class, "anyDate"));
			final var almostTomorrow = YearConstraintFixture.today().plusDays(1);

			// When
			final var valid = validator.isValid(almostTomorrow, null);

			// Then
			assertThat(valid).as("diferença < 1 ano civil → getYears() == 0 com @Year padrão (min Long.MIN_VALUE)").isTrue();
		}

		@Test
		@DisplayName("initialize pode ser chamado novamente para trocar os metadados efetivos")
		void reinitializeSwitchesEffectivePolicy() {
			// Given
			final var validator = new Year.YearValidation();
			final var adult = YearConstraintFixture.yearAnnotation(YearConstraintFixture.AdultStrict.class, "birthDate");
			final var shortWindow = YearConstraintFixture.yearAnnotation(YearConstraintFixture.ShortWindowNullable.class, "referenceDate");
			final var now = YearConstraintFixture.today();
			final var borderline = now.minusYears(3);

			// When / Then — primeiro perfil exige 18–65
			validator.initialize(adult);
			assertThat(validator.isValid(borderline, null)).as("3 anos não satisfaz janela 18–65").isFalse();

			// When / Then — segundo perfil aceita 2–5
			validator.initialize(shortWindow);
			assertThat(validator.isValid(borderline, null)).as("3 anos satisfaz janela 2–5 inclusiva").isTrue();
		}
	}

	private static LocalDate whenYearIsValidated(final LocalDate input) {
		return validate(input);
	}

	private static Set<ConstraintViolation<?>> whenYearIsValidatedExpectingViolations(final LocalDate input, final String assertionDescription) {
		final var thrown = catchThrowable(() -> validate(input));
		assertThat(thrown).as(assertionDescription).isInstanceOf(ConstraintViolationException.class);
		return ((ConstraintViolationException) thrown).getConstraintViolations();
	}

	private static void thenViolationsIncludeYearConstraint(final Set<ConstraintViolation<?>> violations, final String failureDescription) {
		assertThat(violations).as(failureDescription).isNotEmpty();
		assertThat(annotationTypes(violations)).as(failureDescription).contains(Year.class);
	}

	private static void thenSingleViolationTargetsYearProperty(final Set<ConstraintViolation<?>> violations, final String failureDescription) {
		assertThat(violations).as(failureDescription).hasSize(1);
		final var violation = violations.iterator().next();
		assertThat(violation.getPropertyPath().toString()).as(failureDescription).isEqualTo("year");
	}

	private static Set<Class<?>> annotationTypes(final Set<ConstraintViolation<?>> violations) {
		return violations.stream()
				.map(v -> v.getConstraintDescriptor().getAnnotation().annotationType())
				.collect(Collectors.toSet());
	}
}
