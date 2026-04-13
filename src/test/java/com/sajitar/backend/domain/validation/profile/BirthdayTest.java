package com.sajitar.backend.domain.validation.profile;

import static com.sajitar.backend.domain.validation.profile.Birthday.Validation.validate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;

import com.sajitar.backend.domain.validation.Year;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

/**
 * Testes da anotação {@link Birthday} do perfil e de
 * {@link Birthday.Validation#validate(LocalDate)}, no estilo
 * <em>given</em> / <em>when</em> / <em>then</em> alinhado a {@link EmailTest}.
 */
@SpringBootTest
@DisplayName("Anotação @Birthday (perfil)")
public class BirthdayTest {

	@Nested
	@DisplayName("Valores aceitos")
	class AcceptedValues {

		@ParameterizedTest(name = "[{index}] {1}")
		@MethodSource("com.sajitar.backend.domain.validation.profile.BirthdayConstraintFixture#validBirthdayArguments")
		void returnsInputWhenValid(final LocalDate birthday, final String failureDescription) {
			// Given
			final var input = birthday;
			final var description = failureDescription;

			// When
			final var result = whenBirthdayIsValidated(input);

			// Then
			assertThat(result).as(description).isEqualTo(input);
		}
	}

	@Nested
	@DisplayName("Valores rejeitados (nulo)")
	class RejectedNull {

		@ParameterizedTest(name = "[{index}] {1}")
		@MethodSource("com.sajitar.backend.domain.validation.profile.BirthdayConstraintFixture#rejectedNullArguments")
		void throwsWhenNull(final LocalDate birthday, final String failureDescription) {
			// Given
			final var description = failureDescription;

			// When
			final var violations = whenBirthdayIsValidatedExpectingViolations(birthday, description);

			// Then
			assertThat(violations).as(description).isNotEmpty();
			assertThat(annotationTypes(violations)).as(description).contains(jakarta.validation.constraints.NotNull.class);
			thenEveryViolationTargetsBirthdayProperty(violations, description);
		}

		@Test
		@DisplayName("Nulo produz violação @NotNull com caminho de propriedade \"birthday\"")
		void exposesNotNullDescriptorForNullInput() {
			// When
			final var violations = whenBirthdayIsValidatedExpectingViolations(null, "null deve violar @NotNull");

			// Then
			assertThat(violations).hasSize(1);
			final var violation = violations.iterator().next();
			assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType())
					.isEqualTo(jakarta.validation.constraints.NotNull.class);
			assertThat(violation.getInvalidValue()).isNull();
			assertThat(violation.getPropertyPath().toString()).isEqualTo("birthday");
		}
	}

	@Nested
	@DisplayName("Valores rejeitados (idade mínima / data inválida)")
	class RejectedTooYoungOrFuture {

		@ParameterizedTest(name = "[{index}] {1}")
		@MethodSource("com.sajitar.backend.domain.validation.profile.BirthdayConstraintFixture#rejectedTooYoungOrFutureArguments")
		void throwsConstraintViolationWhenAgeBelowMinOrFuture(final LocalDate birthday, final String failureDescription) {
			// Given
			final var input = birthday;
			final var description = failureDescription;

			// When
			final var violations = whenBirthdayIsValidatedExpectingViolations(input, description);

			// Then
			assertThat(violations).as(description).isNotEmpty();
			assertThat(annotationTypes(violations)).as(description).contains(Year.class);
			thenEveryViolationTargetsBirthdayProperty(violations, description);
		}

		@Test
		@DisplayName("Menor de idade: uma violação @Year com a mensagem definida em @Birthday")
		void exposesYearConstraintWithBirthdayMessageWhenTooYoung() {
			// Given
			final var input = BirthdayConstraintFixture.today().minusYears(Birthday.MIN_YEAR - 1);

			// When
			final var violations = whenBirthdayIsValidatedExpectingViolations(
					input,
					"esperada violação de idade mínima (" + Birthday.MIN_YEAR + " anos)");

			// Then
			assertThat(violations).hasSize(1);
			final var violation = violations.iterator().next();
			assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(Year.class);
			assertThat(violation.getMessage()).isEqualTo(BirthdayConstraintFixture.expectedYearViolationMessage());
			assertThat(violation.getInvalidValue()).isEqualTo(input);
			assertThat(violation.getPropertyPath().toString()).isEqualTo("birthday");
		}
	}

	@Nested
	@DisplayName("Comportamento da política (Period.getYears)")
	class PolicyEdgeCases {

		@Test
		@DisplayName("Amanhã como nascimento ainda conta 0 anos civis: falha o mínimo de " + Birthday.MIN_YEAR + " anos")
		void tomorrowAsBirthdayFailsMinAgeBecauseYearComponentIsZero() {
			// Given
			final var input = BirthdayConstraintFixture.today().plusDays(1);

			// When
			final var violations = whenBirthdayIsValidatedExpectingViolations(
					input,
					"futuro sub-anual → getYears() == 0 < " + Birthday.MIN_YEAR);

			// Then
			assertThat(violations).hasSize(1);
			assertThat(annotationTypes(violations)).containsExactly(Year.class);
		}
	}

	private static LocalDate whenBirthdayIsValidated(final LocalDate input) {
		return validate(input);
	}

	private static Set<ConstraintViolation<?>> whenBirthdayIsValidatedExpectingViolations(final LocalDate input, final String assertionDescription) {
		final var thrown = catchThrowable(() -> validate(input));
		assertThat(thrown).as(assertionDescription).isInstanceOf(ConstraintViolationException.class);
		return ((ConstraintViolationException) thrown).getConstraintViolations();
	}

	private static void thenEveryViolationTargetsBirthdayProperty(final Set<ConstraintViolation<?>> violations, final String failureDescription) {
		assertThat(violations).as(failureDescription).isNotEmpty();
		for (final ConstraintViolation<?> v : violations) {
			assertThat(v.getPropertyPath().toString()).as(failureDescription).isEqualTo("birthday");
			if (!Objects.isNull(v.getRootBean())) {
				assertThat(v.getRootBean()).as(failureDescription).isInstanceOf(Birthday.Validation.class);
			}
		}
	}

	private static Set<Class<?>> annotationTypes(final Set<ConstraintViolation<?>> violations) {
		return violations.stream()
				.map(v -> v.getConstraintDescriptor().getAnnotation().annotationType())
				.collect(Collectors.toSet());
	}
}
