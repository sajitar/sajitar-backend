package com.sajitar.backend.domain.validation.profile;

import static com.sajitar.backend.domain.validation.profile.Name.Validation.validate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sajitar.backend.domain.validation.profile.NameConstraintFixture.NameConstraintSample;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

/**
 * Testes da anotação {@link Name} do perfil, organizados no estilo
 * <em>given</em> / <em>when</em> / <em>then</em>
 * (entrada e dados na fase <em>given</em>, execução de
 * {@link Name.Validation#validate(String)} na fase <em>when</em>
 * e asserções na fase <em>then</em>).
 */
@DisplayName("Anotação @Name (perfil)")
public class NameTest {

	@Nested
	@DisplayName("Valores aceitos")
	class AcceptedValues {

		@ParameterizedTest(name = "[{index}] {1}")
		@MethodSource("com.sajitar.backend.domain.validation.profile.NameConstraintFixture#validNameArguments")
		void returnsInputWhenValid(final String name, final String failureDescription) {
			// Given
			final var input = name;
			final var description = failureDescription;

			// When
			final var result = whenNameIsValidated(input);

			// Then
			assertThat(result).as(description).isEqualTo(input);
		}

		@Test
		@DisplayName("Mensagem de violação vem do @Pattern quando o formato é inválido")
		void exposesPatternConstraintDetailsWhenInvalid() {
			// Given
			final NameConstraintSample sample = NameConstraintFixture.namePatternViolation();

			// When
			final var violations = whenNameIsValidatedExpectingViolations(
					sample.sampleInvalidValue(),
					sample.failureDescriptionViolationCount());

			// Then
			thenSingleViolationMatchesConstraint(violations, sample, jakarta.validation.constraints.Pattern.class);
		}

		@Test
		@DisplayName("Mensagem de violação vem do @Size quando excede o máximo permitido")
		void exposesSizeConstraintDetailsWhenTooLong() {
			// Given
			final NameConstraintSample sample = NameConstraintFixture.nameSizeViolation();

			// When
			final var violations = whenNameIsValidatedExpectingViolations(
					sample.sampleInvalidValue(),
					sample.failureDescriptionViolationCount());

			// Then
			thenSingleViolationMatchesConstraint(violations, sample, jakarta.validation.constraints.Size.class);
		}
	}

	@Nested
	@DisplayName("Valores rejeitados (nulos)")
	class RejectedNullOrBlank {

		@ParameterizedTest(name = "[{index}] {1}")
		@MethodSource("com.sajitar.backend.domain.validation.profile.NameConstraintFixture#rejectedNullOrBlankArguments")
		void throwsWhenNull(final String name, final String failureDescription) {
			// Given
			final var input = name;
			final var description = failureDescription;

			// When
			final var violations = whenNameIsValidatedExpectingViolations(input, description);

			// Then
			assertThat(violations).as(description).isNotEmpty();
			if (Objects.isNull(input)) {
				assertThat(annotationTypes(violations)).as(description).contains(jakarta.validation.constraints.NotNull.class);
			}
		}
	}

	@Nested
	@DisplayName("Valores rejeitados (padrão / nome mal formado)")
	class RejectedInvalidPattern {

		@ParameterizedTest(name = "[{index}] {1}")
		@MethodSource("com.sajitar.backend.domain.validation.profile.NameConstraintFixture#invalidPatternArguments")
		void throwsWhenPatternInvalid(final String name, final String failureDescription) {
			// Given
			final var input = name;
			final var description = failureDescription;

			// When
			final var violations = whenNameIsValidatedExpectingViolations(input, description);

			// Then
			thenViolationsIncludePatternConstraint(violations, description);
		}
	}

	/**
	 * Fase <em>when</em> do padrão <em>given</em> / <em>when</em> / <em>then</em>:
	 * executa
	 * {@link Name.Validation#validate(String)} quando o valor deve ser aceito
	 * (fluxo de sucesso).
	 *
	 * @param input nome definido na fase <em>given</em>
	 * @return o mesmo nome retornado por {@code validate} na fase <em>then</em>
	 */
	private static String whenNameIsValidated(final String input) {
		return validate(input);
	}

	/**
	 * Fase <em>when</em> do padrão <em>given</em> / <em>when</em> / <em>then</em>:
	 * executa
	 * {@link Name.Validation#validate(String)} esperando falha e devolve as
	 * violações para a fase <em>then</em>.
	 *
	 * @param input                nome definido na fase <em>given</em>
	 * @param assertionDescription texto (p.ex. vindo da fixture) usado em
	 *                             {@code assertThat(...).as(...)}
	 *                             quando o tipo da exceção não é o esperado
	 * @return violações de constraint contidas na
	 *         {@link ConstraintViolationException}
	 */
	private static Set<ConstraintViolation<?>> whenNameIsValidatedExpectingViolations(final String input, final String assertionDescription) {
		final var thrown = catchThrowable(() -> validate(input));
		assertThat(thrown).as(assertionDescription).isInstanceOf(ConstraintViolationException.class);
		return ((ConstraintViolationException) thrown).getConstraintViolations();
	}

	/**
	 * Fase <em>then</em> do padrão <em>given</em> / <em>when</em> / <em>then</em>:
	 * confere se há violações e se
	 * entre elas está o constraint {@code jakarta.validation.constraints.Pattern}.
	 *
	 * @param violations         resultado obtido na fase <em>when</em>
	 * @param failureDescription descrição associada às asserções (p.ex. da fixture)
	 */
	private static void thenViolationsIncludePatternConstraint(final Set<ConstraintViolation<?>> violations, final String failureDescription) {
		assertThat(violations).as(failureDescription).isNotEmpty();
		assertThat(annotationTypes(violations)).as(failureDescription)
				.contains(jakarta.validation.constraints.Pattern.class);
	}

	/**
	 * Fase <em>then</em> do padrão <em>given</em> / <em>when</em> / <em>then</em>:
	 * confere se há exatamente uma
	 * violação alinhada ao perfil {@code @Name} conforme a fixture
	 * {@link NameConstraintSample}.
	 *
	 * @param violations          resultado obtido na fase <em>when</em>
	 * @param sample              dados esperados carregados na fase <em>given</em> (fixture)
	 * @param constraintAnnotation tipo de anotação de constraint esperado
	 */
	private static void thenSingleViolationMatchesConstraint(
			final Set<ConstraintViolation<?>> violations,
			final NameConstraintSample sample,
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

	/**
	 * Apoio à fase <em>then</em>: obtém os tipos das anotações de constraint
	 * presentes nas violações.
	 *
	 * @param violations conjunto produzido após a fase <em>when</em>
	 * @return tipos de anotação dos descritores de constraint
	 */
	private static Set<Class<?>> annotationTypes(final Set<ConstraintViolation<?>> violations) {
		return violations.stream()
				.map(v -> v.getConstraintDescriptor().getAnnotation().annotationType())
				.collect(Collectors.toSet());
	}
}
