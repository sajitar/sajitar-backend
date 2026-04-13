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

	static LocalDate today() {
		return LocalDate.now(ZoneId.systemDefault());
	}

	static String expectedYearViolationMessage() {
		return "deve ter mais de " + Birthday.MIN_YEAR + " anos";
	}

	static Stream<Arguments> validBirthdayArguments() {
		final var now = today();
		return Stream.of(
				Arguments.of(now.minusYears(Birthday.MIN_YEAR), "exatamente " + Birthday.MIN_YEAR + " anos completos hoje"),
				Arguments.of(now.minusYears(Birthday.MIN_YEAR + 1), "um ano acima do mínimo"),
				Arguments.of(now.minusYears(90), "idade avançada permanece sem teto max explícito em @Birthday"));
	}

	static Stream<Arguments> rejectedTooYoungOrFutureArguments() {
		final var now = today();
		return Stream.of(
				Arguments.of(now.minusYears(Birthday.MIN_YEAR - 1), Birthday.MIN_YEAR - 1 + " anos completos"),
				Arguments.of(now.minusYears(Birthday.MIN_YEAR - 1).minusDays(1), "ainda " + (Birthday.MIN_YEAR - 1) + " anos no calendário civil"),
				Arguments.of(now.plusYears(1), "nascimento pelo menos um ano civil no futuro"));
	}

	static Stream<Arguments> rejectedNullArguments() {
		return Stream.of(Arguments.of(null, "data de nascimento obrigatória (@NotNull)"));
	}
}
