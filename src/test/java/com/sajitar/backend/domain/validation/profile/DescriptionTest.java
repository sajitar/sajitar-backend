package com.sajitar.backend.domain.validation.profile;

import static com.sajitar.backend.domain.validation.profile.Description.Validation.validate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sajitar.backend.domain.validation.profile.DescriptionConstraintFixture.DescriptionConstraintSample;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

/**
 * Testes da anotação {@link Description} do perfil, organizados no estilo
 * <em>given</em> / <em>when</em> / <em>then</em>
 * (entrada e dados na fase <em>given</em>, execução de
 * {@link Description.Validation#validate(String)} na fase <em>when</em>
 * e asserções na fase <em>then</em>).
 */
@DisplayName("Anotação @Description (perfil)")
public class DescriptionTest {

	@Nested
	@DisplayName("Valores aceitos")
	class AcceptedValues {

		@ParameterizedTest(name = "[{index}] {1}")
		@MethodSource("com.sajitar.backend.domain.validation.profile.DescriptionConstraintFixture#validDescriptionArguments")
		void returnsInputWhenValid(final String description, final String failureDescription) {
			// Given
			final var input = description;
			final var assertionDescription = failureDescription;

			// When
			final var result = whenDescriptionIsValidated(input);

			// Then
			assertThat(result).as(assertionDescription).isEqualTo(input);
		}

		@Test
		@DisplayName("Mensagem de violação vem do @Size quando excede o máximo permitido")
		void exposesSizeConstraintDetailsWhenTooLong() {
			// Given
			final DescriptionConstraintSample sample = DescriptionConstraintFixture.descriptionSizeViolation();

			// When
			final var violations = whenDescriptionIsValidatedExpectingViolations(
					sample.sampleInvalidValue(),
					sample.failureDescriptionViolationCount());

			// Then
			thenSingleViolationMatchesSizeConstraint(violations, sample);
		}
	}

	@Nested
	@DisplayName("Valores rejeitados (tamanho acima do máximo)")
	class RejectedExceedsMaxSize {

		@ParameterizedTest(name = "[{index}] {1}")
		@MethodSource("com.sajitar.backend.domain.validation.profile.DescriptionConstraintFixture#exceedsMaxSizeArguments")
		void throwsWhenLongerThanMax(final String description, final String failureDescription) {
			// Given
			final var input = description;
			final var assertionDescription = failureDescription;

			// When
			final var violations = whenDescriptionIsValidatedExpectingViolations(input, assertionDescription);

			// Then
			thenViolationsIncludeSizeConstraint(violations, assertionDescription);
		}
	}

	/**
	 * Fase <em>when</em> do padrão <em>given</em> / <em>when</em> / <em>then</em>:
	 * executa
	 * {@link Description.Validation#validate(String)} quando o valor deve ser aceito
	 * (fluxo de sucesso).
	 *
	 * @param input descrição definida na fase <em>given</em>
	 * @return o mesmo texto retornado por {@code validate} na fase <em>then</em>
	 */
	private static String whenDescriptionIsValidated(final String input) {
		return validate(input);
	}

	/**
	 * Fase <em>when</em> do padrão <em>given</em> / <em>when</em> / <em>then</em>:
	 * executa
	 * {@link Description.Validation#validate(String)} esperando falha e devolve as
	 * violações para a fase <em>then</em>.
	 *
	 * @param input                descrição definida na fase <em>given</em>
	 * @param assertionDescription texto usado em {@code assertThat(...).as(...)}
	 * @return violações de constraint contidas na
	 *         {@link ConstraintViolationException}
	 */
	private static Set<ConstraintViolation<?>> whenDescriptionIsValidatedExpectingViolations(
			final String input,
			final String assertionDescription) {
		final var thrown = catchThrowable(() -> validate(input));
		assertThat(thrown).as(assertionDescription).isInstanceOf(ConstraintViolationException.class);
		return ((ConstraintViolationException) thrown).getConstraintViolations();
	}

	/**
	 * Fase <em>then</em>: violações devem incluir {@code jakarta.validation.constraints.Size}.
	 */
	private static void thenViolationsIncludeSizeConstraint(final Set<ConstraintViolation<?>> violations, final String failureDescription) {
		assertThat(violations).as(failureDescription).isNotEmpty();
		assertThat(annotationTypes(violations)).as(failureDescription)
				.contains(jakarta.validation.constraints.Size.class);
	}

	/**
	 * Fase <em>then</em>: exatamente uma violação {@code @Size} com mensagem e caminho esperados.
	 */
	private static void thenSingleViolationMatchesSizeConstraint(
			final Set<ConstraintViolation<?>> violations,
			final DescriptionConstraintSample sample) {
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
