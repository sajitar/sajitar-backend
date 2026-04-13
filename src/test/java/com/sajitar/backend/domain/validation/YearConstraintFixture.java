package com.sajitar.backend.domain.validation;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

/**
 * Suporte a cenários de {@link Year} / {@link Year.YearValidation}: anotações
 * obtidas por reflexão e datas derivadas de {@link LocalDate#now()} para
 * evitar testes frágeis em relação ao relógio do sistema.
 */
final class YearConstraintFixture {

	private YearConstraintFixture() {
	}

	static LocalDate today() {
		return LocalDate.now(ZoneId.systemDefault());
	}

	static Year yearAnnotation(final Class<?> holder, final String fieldName) {
		try {
			final Field field = holder.getDeclaredField(fieldName);
			final Year annotation = field.getAnnotation(Year.class);
			if (annotation == null) {
				throw new IllegalStateException("Campo " + fieldName + " em " + holder.getName() + " não carrega @Year");
			}
			return annotation;
		} catch (final ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	/** Janela etária típica: 18 a 65 anos, data obrigatória. */
	@SuppressWarnings("unused")
	static final class AdultStrict {
		@Year(min = 18, max = 65, nullable = false)
		LocalDate birthDate;
	}

	/** Intervalo curto para limites “exatos” em anos completos. */
	@SuppressWarnings("unused")
	static final class ShortWindowNullable {
		@Year(min = 2, max = 5, nullable = true)
		LocalDate referenceDate;
	}

	/** Somente {@code nullable = false} com min/max padrão (Long). */
	@SuppressWarnings("unused")
	static final class NonNullDefaultRange {
		@Year(nullable = false)
		LocalDate birthDate;
	}

	/** Metadados padrão da anotação (min/max extremos, nullable true). */
	@SuppressWarnings("unused")
	static final class DefaultYearMeta {
		@Year
		LocalDate anyDate;
	}

	static Stream<Arguments> validationAcceptedArguments() {
		final var now = today();
		return Stream.of(
				Arguments.of(null, "null deve ser aceito (nullable = true na fábrica Year.Validation)"),
				Arguments.of(now, "hoje implica idade 0 anos, dentro de [0, 150]"),
				Arguments.of(now.minusYears(75), "idade intermediária dentro da janela"),
				Arguments.of(now.minusYears(150), "limite superior exato de 150 anos completos"));
	}

	static Stream<Arguments> validationRejectedArguments() {
		final var now = today();
		return Stream.of(
				Arguments.of(now.plusYears(1), "data pelo menos ~1 ano no futuro deixa getYears() negativo, fora de min = 0"),
				Arguments.of(now.minusYears(151), "151 anos completos excede max = 150"));
	}

	static Stream<Arguments> yearValidationAdultStrictAcceptedArguments() {
		final var now = today();
		return Stream.of(
				Arguments.of(now.minusYears(18), "completou 18 anos hoje (mesmo mês/dia)"),
				Arguments.of(now.minusYears(65), "completou 65 anos hoje, ainda dentro do máximo"),
				Arguments.of(now.minusYears(40), "idade intermediária na janela"));
	}

	static Stream<Arguments> yearValidationAdultStrictRejectedArguments() {
		final var now = today();
		return Stream.of(
				Arguments.of(null, "nullable = false rejeita ausência de data"),
				Arguments.of(now.minusYears(17).minusDays(1), "um dia antes de completar 18 anos"),
				Arguments.of(now.minusYears(66), "excede 65 anos completos"),
				Arguments.of(now.plusYears(1), "data no próximo ano civil deixa anos completos negativos frente a min = 18"));
	}

	static Stream<Arguments> yearValidationShortWindowBoundaryArguments() {
		final var now = today();
		final Year meta = yearAnnotation(ShortWindowNullable.class, "referenceDate");
		return Stream.of(
				Arguments.of(meta, now.minusYears(2), true, "exatamente 2 anos completos (mínimo inclusivo)"),
				Arguments.of(meta, now.minusYears(5), true, "exatamente 5 anos completos (máximo inclusivo)"),
				Arguments.of(meta, now.minusYears(1).minusDays(1), false, "ainda não completou 2 anos"),
				Arguments.of(meta, now.minusYears(6), false, "já ultrapassou 5 anos completos"),
				Arguments.of(meta, null, true, "nullable = true aceita null"));
	}

	static Stream<Arguments> yearValidationNonNullDefaultRangeArguments() {
		final var now = today();
		final Year meta = yearAnnotation(NonNullDefaultRange.class, "birthDate");
		return Stream.of(
				Arguments.of(meta, null, false, "nullable = false com min/max padrão ainda rejeita null"),
				Arguments.of(meta, now.minusYears(500), true, "idade enorme permanece dentro do max padrão Long.MAX_VALUE"),
				Arguments.of(meta, now.plusYears(1), true, "data futura: anos negativos ainda >= Long.MIN_VALUE"));
	}

	static Stream<Arguments> yearValidationDefaultMetaArguments() {
		final var now = today();
		final Year meta = yearAnnotation(DefaultYearMeta.class, "anyDate");
		return Stream.of(
				Arguments.of(meta, null, true, "nullable padrão true"),
				Arguments.of(meta, now.plusYears(10), true, "min/max padrão aceitam anos negativos"),
				Arguments.of(meta, now.minusYears(10_000), true, "min/max padrão aceitam idades muito altas"));
	}

	static String describeViolationSample(final LocalDate rejected, final String suffix) {
		return "violação esperada para " + Objects.toString(rejected, "null") + " — " + suffix;
	}
}
