package com.sajitar.backend.domain.validation.profile;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

/**
 * Cenários para {@link Birthday} / {@link Birthday.Validation#validate(LocalDate)}
 * com datas relativas a {@code LocalDate.now(ZoneId.systemDefault())}.
 */
final class BirthdayConstraintFixture {

	private static final int minAgeYears = 18;

	static LocalDate today() {
		return LocalDate.now(ZoneId.systemDefault());
	}

	static String expectedBirthdayViolationMessage() {
		return "deve ter mais de " + minAgeYears + " anos";
	}

	static Stream<Arguments> validBirthdayArguments() {
		final var now = today();
		final int minYears = minAgeYears;
		return Stream.of(
				Arguments.of(now.minusYears(minYears), "exatamente " + minYears + " anos completos hoje"),
				Arguments.of(now.minusYears(minYears + 1), "um ano acima do mínimo"),
				Arguments.of(now.minusYears(90), "idade avançada permanece sem teto max explícito em @Birthday"));
	}

	static Stream<Arguments> rejectedTooYoungOrFutureArguments() {
		final var now = today();
		final int minYears = minAgeYears;
		return Stream.of(
				Arguments.of(now.minusYears(minYears - 1), minYears - 1 + " anos completos"),
				Arguments.of(now.minusYears(minYears - 1).minusDays(1), "ainda " + (minYears - 1) + " anos no calendário civil"),
				Arguments.of(now.plusYears(1), "nascimento pelo menos um ano civil no futuro"));
	}

	static Stream<Arguments> rejectedNullArguments() {
		return Stream.of(Arguments.of(null, "data de nascimento obrigatória (@NotNull)"));
	}
}
