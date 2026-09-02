package com.sajitar.backend.domain.validation.profile;

import static com.sajitar.backend.domain.validation.profile.Email.Validation.validate;
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

import com.sajitar.backend.domain.validation.profile.EmailConstraintFixture.EmailSizeViolation;
import com.sajitar.backend.domain.validation.profile.EmailConstraintFixture.JakartaEmailViolation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

/**
 * Testes da anotação {@link Email} do perfil, organizados no estilo
 * <em>given</em> / <em>when</em> / <em>then</em>
 * (entrada e dados na fase <em>given</em>, execução de
 * {@link Email.Validation#validate(String)} na fase <em>when</em>
 * e asserções na fase <em>then</em>).
 */
@DisplayName("Anotação @Email (perfil)")
public class EmailTest {

	@Nested
	@DisplayName("Valores aceitos")
	class AcceptedValues {

		@ParameterizedTest(name = "[{index}] {1}")
		@MethodSource("com.sajitar.backend.domain.validation.profile.EmailConstraintFixture#validEmailArguments")
		void returnsInputWhenValid(final String email, final String failureDescription) {
			// Given
			final var input = email;
			final var description = failureDescription;

			// When
			final var result = whenEmailIsValidated(input);

			// Then
			assertThat(result).as(description).isEqualTo(input);
		}

		@Test
		@DisplayName("Mensagem de violação vem do @Email Jakarta (anotação composta não sobrescreve o nested)")
		void exposesJakartaEmailConstraintDetailsWhenInvalid() {
			// Given
			final JakartaEmailViolation sample = EmailConstraintFixture.jakartaEmailViolation();

			// When
			final var violations = whenEmailIsValidatedExpectingViolations(
					sample.sampleInvalidValue(),
					sample.failureDescriptionViolationCount());

			// Then
			thenSingleViolationMatchesJakartaEmailConstraint(violations, sample);
		}

		@Test
		@DisplayName("Mensagem de violação vem do @Size quando excede o máximo permitido")
		void exposesSizeConstraintDetailsWhenTooLong() {
			// Given
			final EmailSizeViolation sample = EmailConstraintFixture.emailSizeViolation();

			// When
			final var violations = whenEmailIsValidatedExpectingViolations(
					sample.sampleInvalidValue(),
					sample.failureDescriptionViolationCount());

			// Then
			thenSingleViolationMatchesEmailSizeConstraint(violations, sample);
		}
	}

	@Nested
	@DisplayName("Valores rejeitados (nulos ou vazios)")
	class RejectedNullOrBlank {

		@ParameterizedTest(name = "[{index}] {1}")
		@MethodSource("com.sajitar.backend.domain.validation.profile.EmailConstraintFixture#rejectedNullOrBlankArguments")
		void throwsWhenNullOrEmpty(final String email, final String failureDescription) {
			// Given
			final var input = email;
			final var description = failureDescription;

			// When
			final var violations = whenEmailIsValidatedExpectingViolations(input, description);

			// Then
			assertThat(violations).as(description).isNotEmpty();
			if (Objects.isNull(input)) {
				assertThat(annotationTypes(violations)).as(description).contains(jakarta.validation.constraints.NotNull.class);
			}
		}
	}

	@Nested
	@DisplayName("Valores rejeitados (formato / política da regex)")
	class RejectedInvalidFormat {

		@ParameterizedTest(name = "[{index}] {1}")
		@MethodSource("com.sajitar.backend.domain.validation.profile.EmailConstraintFixture#invalidFormatArguments")
		void throwsWhenFormatInvalid(final String email, final String failureDescription) {
			// Given
			final var input = email;
			final var description = failureDescription;

			// When
			final var violations = whenEmailIsValidatedExpectingViolations(input, description);

			// Then
			thenViolationsIncludeJakartaEmailConstraint(violations, description);
		}
	}

	@Nested
	@DisplayName("Valores rejeitados (tamanho acima do máximo)")
	class RejectedExceedsMaxSize {

		@ParameterizedTest(name = "[{index}] {1}")
		@MethodSource("com.sajitar.backend.domain.validation.profile.EmailConstraintFixture#exceedsMaxSizeArguments")
		void throwsWhenLongerThanMax(final String email, final String failureDescription) {
			// Given
			final var input = email;
			final var description = failureDescription;

			// When
			final var violations = whenEmailIsValidatedExpectingViolations(input, description);

			// Then
			thenViolationsIncludeSizeConstraint(violations, description);
		}
	}

	/**
	 * Fase <em>when</em> do padrão <em>given</em> / <em>when</em> / <em>then</em>:
	 * executa
	 * {@link Email.Validation#validate(String)} quando o valor deve ser aceito
	 * (fluxo de sucesso).
	 *
	 * @param input endereço definido na fase <em>given</em>
	 * @return o mesmo endereço retornado por {@code validate} na fase <em>then</em>
	 */
	private static String whenEmailIsValidated(final String input) {
		return validate(input);
	}

	/**
	 * Fase <em>when</em> do padrão <em>given</em> / <em>when</em> / <em>then</em>:
	 * executa
	 * {@link Email.Validation#validate(String)} esperando falha e devolve as
	 * violações para a fase <em>then</em>.
	 *
	 * @param input                endereço definido na fase <em>given</em>
	 * @param assertionDescription texto (p.ex. vindo da fixture) usado em
	 *                             {@code assertThat(...).as(...)}
	 *                             quando o tipo da exceção não é o esperado
	 * @return violações de constraint contidas na
	 *         {@link ConstraintViolationException}
	 */
	private static Set<ConstraintViolation<?>> whenEmailIsValidatedExpectingViolations(final String input, final String assertionDescription) {
		final var thrown = catchThrowable(() -> validate(input));
		assertThat(thrown).as(assertionDescription).isInstanceOf(ConstraintViolationException.class);
		return ((ConstraintViolationException) thrown).getConstraintViolations();
	}

	/**
	 * Fase <em>then</em> do padrão <em>given</em> / <em>when</em> / <em>then</em>:
	 * confere se há violações e se
	 * entre elas está o constraint {@code jakarta.validation.constraints.Email}.
	 *
	 * @param violations         resultado obtido na fase <em>when</em>
	 * @param failureDescription descrição associada às asserções (p.ex. da fixture)
	 */
	private static void thenViolationsIncludeJakartaEmailConstraint(final Set<ConstraintViolation<?>> violations, final String failureDescription) {
		assertThat(violations).as(failureDescription).isNotEmpty();
		assertThat(annotationTypes(violations)).as(failureDescription)
				.contains(jakarta.validation.constraints.Email.class);
	}

	private static void thenViolationsIncludeSizeConstraint(final Set<ConstraintViolation<?>> violations, final String failureDescription) {
		assertThat(violations).as(failureDescription).isNotEmpty();
		assertThat(annotationTypes(violations)).as(failureDescription)
				.contains(jakarta.validation.constraints.Size.class);
	}

	/**
	 * Fase <em>then</em> do padrão <em>given</em> / <em>when</em> / <em>then</em>:
	 * confere se há exatamente uma
	 * violação alinhada ao perfil {@code @Email} Jakarta conforme a fixture
	 * {@link EmailConstraintFixture.JakartaEmailViolation}.
	 *
	 * @param violations resultado obtido na fase <em>when</em>
	 * @param sample     dados esperados carregados na fase <em>given</em> (fixture)
	 */
	private static void thenSingleViolationMatchesJakartaEmailConstraint(final Set<ConstraintViolation<?>> violations, final JakartaEmailViolation sample) {
		assertThat(violations).as(sample.failureDescriptionViolationCount()).hasSize(1);
		final var violation = violations.iterator().next();
		assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType())
				.as(sample.failureDescriptionConstraintAnnotation())
				.isEqualTo(jakarta.validation.constraints.Email.class);
		assertThat(violation.getMessage())
				.as(sample.failureDescriptionMessage())
				.isEqualTo(sample.expectedMessagePtBr());
		assertThat(violation.getPropertyPath().toString())
				.as(sample.failureDescriptionPropertyPath())
				.isEqualTo(sample.expectedPropertyPath());
	}

	private static void thenSingleViolationMatchesEmailSizeConstraint(final Set<ConstraintViolation<?>> violations, final EmailSizeViolation sample) {
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
