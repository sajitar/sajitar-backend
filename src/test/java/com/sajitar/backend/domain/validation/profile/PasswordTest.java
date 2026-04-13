package com.sajitar.backend.domain.validation.profile;

import static com.sajitar.backend.domain.validation.profile.Password.Validation.validate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;

import com.sajitar.backend.domain.validation.profile.PasswordConstraintFixture.PasswordMaxSizeViolation;
import com.sajitar.backend.domain.validation.profile.PasswordConstraintFixture.PasswordMinSizeViolation;
import com.sajitar.backend.domain.validation.profile.PasswordConstraintFixture.PasswordNotNullViolation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

/**
 * Testes da anotação {@link Password} do perfil, organizados no estilo
 * <em>given</em> / <em>when</em> / <em>then</em>
 * (entrada e dados na fase <em>given</em>, execução de
 * {@link Password.Validation#validate(String)} na fase <em>when</em>
 * e asserções na fase <em>then</em>), no mesmo espírito da fixture JSON da
 * {@link Description} em {@code /fixtures/description-validation.json} e da
 * suíte {@link EmailTest} / {@link EmailConstraintFixture}.
 */
@SpringBootTest
@DisplayName("Anotação @Password (perfil)")
public class PasswordTest {

	@Nested
	@DisplayName("Valores aceitos")
	class AcceptedValues {

		@ParameterizedTest(name = "[{index}] {1}")
		@MethodSource("com.sajitar.backend.domain.validation.profile.PasswordConstraintFixture#validPasswordArguments")
		void returnsInputWhenValid(final String password, final String failureDescription) {
			// Given
			final var input = password;
			final var description = failureDescription;

			// When
			final var result = whenPasswordIsValidated(input);

			// Then
			assertThat(result).as(description).isEqualTo(input);
		}

		@Test
		@DisplayName("Mensagem de violação vem do @NotNull quando o valor é null")
		void exposesNotNullConstraintDetailsWhenNull() {
			// Given
			final PasswordNotNullViolation sample = PasswordConstraintFixture.passwordNotNullViolation();

			// When
			final var violations = whenPasswordIsValidatedExpectingViolations(
					sample.sampleInvalidValue(),
					sample.failureDescriptionViolationCount());

			// Then
			thenSingleViolationMatchesNotNullConstraint(violations, sample);
		}

		@Test
		@DisplayName("Mensagem de violação vem do @Size quando abaixo do mínimo permitido")
		void exposesSizeConstraintDetailsWhenBelowMin() {
			// Given
			final PasswordMinSizeViolation sample = PasswordConstraintFixture.passwordMinSizeViolation();

			// When
			final var violations = whenPasswordIsValidatedExpectingViolations(
					sample.sampleInvalidValue(),
					sample.failureDescriptionViolationCount());

			// Then
			thenSingleViolationMatchesPasswordMinSizeConstraint(violations, sample);
		}

		@Test
		@DisplayName("Mensagem de violação vem do @Size quando excede o máximo permitido")
		void exposesSizeConstraintDetailsWhenAboveMax() {
			// Given
			final PasswordMaxSizeViolation sample = PasswordConstraintFixture.passwordMaxSizeViolation();

			// When
			final var violations = whenPasswordIsValidatedExpectingViolations(
					sample.sampleInvalidValue(),
					sample.failureDescriptionViolationCount());

			// Then
			thenSingleViolationMatchesPasswordMaxSizeConstraint(violations, sample);
		}
	}

	@Nested
	@DisplayName("Valores rejeitados (nulos)")
	class RejectedNull {

		@ParameterizedTest(name = "[{index}] {1}")
		@MethodSource("com.sajitar.backend.domain.validation.profile.PasswordConstraintFixture#rejectedNullArguments")
		void throwsWhenNull(final String password, final String failureDescription) {
			// Given
			final var input = password;
			final var description = failureDescription;

			// When
			final var violations = whenPasswordIsValidatedExpectingViolations(input, description);

			// Then
			assertThat(violations).as(description).isNotEmpty();
			assertThat(annotationTypes(violations)).as(description).contains(jakarta.validation.constraints.NotNull.class);
		}
	}

	@Nested
	@DisplayName("Valores rejeitados (abaixo do tamanho mínimo)")
	class RejectedBelowMinSize {

		@ParameterizedTest(name = "[{index}] {1}")
		@MethodSource("com.sajitar.backend.domain.validation.profile.PasswordConstraintFixture#belowMinSizeArguments")
		void throwsWhenShorterThanMin(final String password, final String failureDescription) {
			// Given
			final var input = password;
			final var description = failureDescription;

			// When
			final var violations = whenPasswordIsValidatedExpectingViolations(input, description);

			// Then
			thenViolationsIncludeSizeConstraint(violations, description);
		}
	}

	@Nested
	@DisplayName("Valores rejeitados (acima do tamanho máximo)")
	class RejectedExceedsMaxSize {

		@ParameterizedTest(name = "[{index}] {1}")
		@MethodSource("com.sajitar.backend.domain.validation.profile.PasswordConstraintFixture#exceedsMaxSizeArguments")
		void throwsWhenLongerThanMax(final String password, final String failureDescription) {
			// Given
			final var input = password;
			final var description = failureDescription;

			// When
			final var violations = whenPasswordIsValidatedExpectingViolations(input, description);

			// Then
			thenViolationsIncludeSizeConstraint(violations, description);
		}
	}

	private static String whenPasswordIsValidated(final String input) {
		return validate(input);
	}

	private static Set<ConstraintViolation<?>> whenPasswordIsValidatedExpectingViolations(final String input,
			final String assertionDescription) {
		final var thrown = catchThrowable(() -> validate(input));
		assertThat(thrown).as(assertionDescription).isInstanceOf(ConstraintViolationException.class);
		return ((ConstraintViolationException) thrown).getConstraintViolations();
	}

	private static void thenViolationsIncludeSizeConstraint(final Set<ConstraintViolation<?>> violations,
			final String failureDescription) {
		assertThat(violations).as(failureDescription).isNotEmpty();
		assertThat(annotationTypes(violations)).as(failureDescription)
				.contains(jakarta.validation.constraints.Size.class);
	}

	private static void thenSingleViolationMatchesNotNullConstraint(final Set<ConstraintViolation<?>> violations,
			final PasswordNotNullViolation sample) {
		assertThat(violations).as(sample.failureDescriptionViolationCount()).hasSize(1);
		final var violation = violations.iterator().next();
		assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType())
				.as(sample.failureDescriptionConstraintAnnotation())
				.isEqualTo(jakarta.validation.constraints.NotNull.class);
		assertThat(violation.getMessage())
				.as(sample.failureDescriptionMessage())
				.isEqualTo(sample.expectedMessagePtBr());
		assertThat(violation.getPropertyPath().toString())
				.as(sample.failureDescriptionPropertyPath())
				.isEqualTo(sample.expectedPropertyPath());
		assertThat(sample.sampleInvalidValue()).as(sample.failureDescriptionViolationCount()).isNull();
	}

	private static void thenSingleViolationMatchesPasswordMinSizeConstraint(final Set<ConstraintViolation<?>> violations,
			final PasswordMinSizeViolation sample) {
		assertThat(violations).as(sample.failureDescriptionViolationCount()).hasSize(1);
		final var violation = violations.iterator().next();
		assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType())
				.as(sample.failureDescriptionConstraintAnnotation())
				.isEqualTo(jakarta.validation.constraints.Size.class);
		assertThat(violation.getMessage())
				.as(sample.failureDescriptionMessage())
				.isEqualTo(sample.expectedMessagePtBr());
		assertThat(violation.getPropertyPath().toString())
				.as(sample.failureDescriptionPropertyPath())
				.isEqualTo(sample.expectedPropertyPath());
	}

	private static void thenSingleViolationMatchesPasswordMaxSizeConstraint(final Set<ConstraintViolation<?>> violations,
			final PasswordMaxSizeViolation sample) {
		assertThat(violations).as(sample.failureDescriptionViolationCount()).hasSize(1);
		final var violation = violations.iterator().next();
		assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType())
				.as(sample.failureDescriptionConstraintAnnotation())
				.isEqualTo(jakarta.validation.constraints.Size.class);
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
