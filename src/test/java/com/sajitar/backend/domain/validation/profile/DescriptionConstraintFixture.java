package com.sajitar.backend.domain.validation.profile;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;

/**
 * Carrega exemplos compartilhados de validação de descrição a partir de
 * {@code /fixtures/description-validation.json} e acrescenta casos de limite
 * de tamanho gerados em memória (500 e 501 caracteres).
 */
final class DescriptionConstraintFixture {

	private static final String RESOURCE = "/fixtures/description-validation.json";

	private static final List<DescriptionScenario> VALID;
	private static final List<DescriptionScenario> EXCEEDS_MAX_SIZE;
	private static final DescriptionConstraintSample DESCRIPTION_SIZE_VIOLATION;

	static {
		try (var in = DescriptionConstraintFixture.class.getResourceAsStream(RESOURCE)) {
			if (in == null) {
				throw new IllegalStateException("Missing classpath resource: " + RESOURCE);
			}
			final var parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
			final var root = (JSONObject) parser.parse(new InputStreamReader(in, StandardCharsets.UTF_8));

			final var valid = new ArrayList<>(toDescriptionScenarios((JSONArray) root.get("valid")));
			valid.add(new DescriptionScenario(
					"F".repeat(Description.MAX_SIZE),
					"Descrição com exatamente " + Description.MAX_SIZE + " caracteres (limite @Size) deveria ser aceita"));
			VALID = List.copyOf(valid);

			final var exceeds = new ArrayList<DescriptionScenario>();
			final var exceedsJson = (JSONArray) root.get("exceedsMaxSize");
			if (exceedsJson != null) {
				exceeds.addAll(toDescriptionScenarios(exceedsJson));
			}
			exceeds.add(new DescriptionScenario(
					"G".repeat(Description.MAX_SIZE + 1),
					"Descrição com " + (Description.MAX_SIZE + 1) + " caracteres deveria violar @Size"));
			EXCEEDS_MAX_SIZE = List.copyOf(exceeds);

			final var sizeBlock = (JSONObject) root.get("descriptionSizeViolation");
			final var failures = (JSONObject) sizeBlock.get("failureDescriptions");
			final int invalidLength = ((Number) sizeBlock.get("invalidLength")).intValue();
			DESCRIPTION_SIZE_VIOLATION = new DescriptionConstraintSample(
					"H".repeat(invalidLength),
					Objects.requireNonNull((String) sizeBlock.get("expectedMessagePtBr")),
					Objects.requireNonNull((String) sizeBlock.get("expectedPropertyPath")),
					Objects.requireNonNull((String) failures.get("violationCount")),
					Objects.requireNonNull((String) failures.get("constraintAnnotation")),
					Objects.requireNonNull((String) failures.get("message")),
					Objects.requireNonNull((String) failures.get("propertyPath")));
		} catch (final Exception e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	static Stream<Arguments> validDescriptionArguments() {
		return VALID.stream().map(s -> Arguments.of(s.description(), s.failureDescription()));
	}

	static Stream<Arguments> exceedsMaxSizeArguments() {
		return EXCEEDS_MAX_SIZE.stream().map(s -> Arguments.of(s.description(), s.failureDescription()));
	}

	static DescriptionConstraintSample descriptionSizeViolation() {
		return DESCRIPTION_SIZE_VIOLATION;
	}

	private static List<DescriptionScenario> toDescriptionScenarios(final JSONArray array) {
		return array.stream().map(DescriptionConstraintFixture::toDescriptionScenario).toList();
	}

	private static DescriptionScenario toDescriptionScenario(final Object element) {
		final var obj = (JSONObject) element;
		final Object raw = obj.get("description");
		final String description = raw == null ? null : (String) raw;
		final String failureDescription = Objects.requireNonNull((String) obj.get("failureDescription"));
		return new DescriptionScenario(description, failureDescription);
	}

	record DescriptionScenario(String description, String failureDescription) {
	}

	record DescriptionConstraintSample(
			String sampleInvalidValue,
			String expectedMessagePtBr,
			String expectedPropertyPath,
			String failureDescriptionViolationCount,
			String failureDescriptionConstraintAnnotation,
			String failureDescriptionMessage,
			String failureDescriptionPropertyPath) {
	}
}
