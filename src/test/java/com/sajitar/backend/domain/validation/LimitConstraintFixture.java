package com.sajitar.backend.domain.validation;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;

/**
 * Carrega exemplos compartilhados de validação de limite numérico a partir de
 * {@code /fixtures/limit-validation.json}.
 */
final class LimitConstraintFixture {

	private static final String RESOURCE = "/fixtures/limit-validation.json";

	private static final List<LimitScenario> VALID;
	private static final List<LimitScenario> NOT_POSITIVE;
	private static final List<LimitScenario> EXCEEDS_MAX;
	private static final List<LimitScenario> REJECTED_NULL_OR_BLANK;
	private static final LimitConstraintSample POSITIVE_VIOLATION;
	private static final LimitConstraintSample MAX_VIOLATION;

	static {
		try (var in = LimitConstraintFixture.class.getResourceAsStream(RESOURCE)) {
			if (in == null) {
				throw new IllegalStateException("Missing classpath resource: " + RESOURCE);
			}
			final var parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
			final var root = (JSONObject) parser.parse(new InputStreamReader(in, StandardCharsets.UTF_8));

			VALID = toLimitScenarios((JSONArray) root.get("valid"));
			NOT_POSITIVE = toLimitScenarios((JSONArray) root.get("notPositive"));
			EXCEEDS_MAX = toLimitScenarios((JSONArray) root.get("exceedsMax"));
			REJECTED_NULL_OR_BLANK = toLimitScenarios((JSONArray) root.get("rejectedNullOrBlank"));

			POSITIVE_VIOLATION = toConstraintSample((JSONObject) root.get("positiveConstraintViolation"));
			MAX_VIOLATION = toConstraintSample((JSONObject) root.get("maxConstraintViolation"));
			assertPositiveFixtureConsistent(POSITIVE_VIOLATION);
			assertMaxFixtureConsistent(MAX_VIOLATION);
		} catch (final Exception e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	static Stream<Arguments> validLimitArguments() {
		return VALID.stream().map(s -> Arguments.of(s.limit(), s.failureDescription()));
	}

	static Stream<Arguments> notPositiveArguments() {
		return NOT_POSITIVE.stream().map(s -> Arguments.of(s.limit(), s.failureDescription()));
	}

	static Stream<Arguments> exceedsMaxArguments() {
		return EXCEEDS_MAX.stream().map(s -> Arguments.of(s.limit(), s.failureDescription()));
	}

	static Stream<Arguments> rejectedNullOrBlankArguments() {
		return REJECTED_NULL_OR_BLANK.stream().map(s -> Arguments.of(s.limit(), s.failureDescription()));
	}

	static LimitConstraintSample positiveConstraintViolation() {
		return POSITIVE_VIOLATION;
	}

	static LimitConstraintSample maxConstraintViolation() {
		return MAX_VIOLATION;
	}

	private static void assertPositiveFixtureConsistent(final LimitConstraintSample sample) {
		if (sample.sampleInvalidValue() == null || sample.sampleInvalidValue() <= 0) {
			return;
		}
		throw new IllegalStateException(
				"Fixture positiveConstraintViolation.sampleInvalidValue deve ser <= 0 para exercitar @Limit (não positivo), obtido "
						+ sample.sampleInvalidValue());
	}

	private static void assertMaxFixtureConsistent(final LimitConstraintSample sample) {
		if (sample.sampleInvalidValue() != null && sample.sampleInvalidValue() > 100) {
			return;
		}
		throw new IllegalStateException(
				"Fixture maxConstraintViolation.sampleInvalidValue deve ser > 100 (teto em application.yml de teste), obtido "
						+ sample.sampleInvalidValue());
	}

	private static LimitConstraintSample toConstraintSample(final JSONObject block) {
		final var failures = (JSONObject) block.get("failureDescriptions");
		return new LimitConstraintSample(
				requireInteger(block.get("sampleInvalidValue")),
				Objects.requireNonNull((String) block.get("expectedMessagePtBr")),
				Objects.requireNonNull((String) block.get("expectedPropertyPath")),
				Objects.requireNonNull((String) failures.get("violationCount")),
				Objects.requireNonNull((String) failures.get("constraintAnnotation")),
				Objects.requireNonNull((String) failures.get("message")),
				Objects.requireNonNull((String) failures.get("propertyPath")));
	}

	private static List<LimitScenario> toLimitScenarios(final JSONArray array) {
		return array.stream().map(LimitConstraintFixture::toLimitScenario).toList();
	}

	private static LimitScenario toLimitScenario(final Object element) {
		final var obj = (JSONObject) element;
		final Integer limit = toNullableInteger(obj.get("limit"));
		final String failureDescription = Objects.requireNonNull((String) obj.get("failureDescription"));
		return new LimitScenario(limit, failureDescription);
	}

	private static Integer toNullableInteger(final Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof final Number n) {
			return n.intValue();
		}
		throw new IllegalArgumentException("Esperado número ou null para limit, obtido: " + raw.getClass());
	}

	private static Integer requireInteger(final Object raw) {
		final Integer v = toNullableInteger(raw);
		return Objects.requireNonNull(v, "sampleInvalidValue não pode ser null nesta fixture");
	}

	record LimitScenario(Integer limit, String failureDescription) {
	}

	record LimitConstraintSample(
			Integer sampleInvalidValue,
			String expectedMessagePtBr,
			String expectedPropertyPath,
			String failureDescriptionViolationCount,
			String failureDescriptionConstraintAnnotation,
			String failureDescriptionMessage,
			String failureDescriptionPropertyPath) {
	}
}
