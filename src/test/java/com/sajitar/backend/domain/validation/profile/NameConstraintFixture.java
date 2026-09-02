package com.sajitar.backend.domain.validation.profile;

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
 * Carrega exemplos compartilhados de validação de nome a partir de
 * {@code /fixtures/name-validation.json}.
 */
final class NameConstraintFixture {

	private static final String RESOURCE = "/fixtures/name-validation.json";

	private static final List<NameScenario> VALID;
	private static final List<NameScenario> INVALID_PATTERN;
	private static final List<NameScenario> REJECTED_NULL_OR_BLANK;
	private static final NameConstraintSample NAME_PATTERN_VIOLATION;
	private static final NameConstraintSample NAME_SIZE_VIOLATION;

	static {
		try (var in = NameConstraintFixture.class.getResourceAsStream(RESOURCE)) {
			if (in == null) {
				throw new IllegalStateException("Missing classpath resource: " + RESOURCE);
			}
			final var parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
			final var root = (JSONObject) parser.parse(new InputStreamReader(in, StandardCharsets.UTF_8));

			VALID = toNameScenarios((JSONArray) root.get("valid"));
			INVALID_PATTERN = toNameScenarios((JSONArray) root.get("invalidPattern"));
			REJECTED_NULL_OR_BLANK = toNameScenarios((JSONArray) root.get("rejectedNullOrBlank"));

			NAME_PATTERN_VIOLATION = toConstraintSample((JSONObject) root.get("namePatternViolation"));
			NAME_SIZE_VIOLATION = toConstraintSample((JSONObject) root.get("nameSizeViolation"));
		} catch (final Exception e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	static Stream<Arguments> validNameArguments() {
		return VALID.stream().map(s -> Arguments.of(s.name(), s.failureDescription()));
	}

	static Stream<Arguments> invalidPatternArguments() {
		return INVALID_PATTERN.stream().map(s -> Arguments.of(s.name(), s.failureDescription()));
	}

	static Stream<Arguments> rejectedNullOrBlankArguments() {
		return REJECTED_NULL_OR_BLANK.stream().map(s -> Arguments.of(s.name(), s.failureDescription()));
	}

	static NameConstraintSample namePatternViolation() {
		return NAME_PATTERN_VIOLATION;
	}

	static NameConstraintSample nameSizeViolation() {
		return NAME_SIZE_VIOLATION;
	}

	private static NameConstraintSample toConstraintSample(final JSONObject jakarta) {
		final var failures = (JSONObject) jakarta.get("failureDescriptions");
		return new NameConstraintSample(
				Objects.requireNonNull((String) jakarta.get("sampleInvalidValue")),
				Objects.requireNonNull((String) jakarta.get("expectedMessagePtBr")),
				Objects.requireNonNull((String) jakarta.get("expectedPropertyPath")),
				Objects.requireNonNull((String) failures.get("violationCount")),
				Objects.requireNonNull((String) failures.get("constraintAnnotation")),
				Objects.requireNonNull((String) failures.get("message")),
				Objects.requireNonNull((String) failures.get("propertyPath")));
	}

	private static List<NameScenario> toNameScenarios(final JSONArray array) {
		return array.stream().map(NameConstraintFixture::toNameScenario).toList();
	}

	private static NameScenario toNameScenario(final Object element) {
		final var obj = (JSONObject) element;
		final Object rawName = obj.get("name");
		final String name = rawName == null ? null : (String) rawName;
		final String failureDescription = Objects.requireNonNull((String) obj.get("failureDescription"));
		return new NameScenario(name, failureDescription);
	}

	record NameScenario(String name, String failureDescription) {
	}

	record NameConstraintSample(
			String sampleInvalidValue,
			String expectedMessagePtBr,
			String expectedPropertyPath,
			String failureDescriptionViolationCount,
			String failureDescriptionConstraintAnnotation,
			String failureDescriptionMessage,
			String failureDescriptionPropertyPath) {
	}
}
