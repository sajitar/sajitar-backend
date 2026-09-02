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
 * Carrega exemplos compartilhados de validação de senha a partir de
 * {@code /fixtures/password-validation.json}.
 */
final class PasswordConstraintFixture {

	private static final String RESOURCE = "/fixtures/password-validation.json";

	private static final List<PasswordScenario> VALID;
	private static final List<PasswordScenario> REJECTED_NULL;
	private static final List<PasswordScenario> BELOW_MIN_SIZE;
	private static final List<PasswordScenario> EXCEEDS_MAX_SIZE;
	private static final PasswordNotNullViolation PASSWORD_NOT_NULL_VIOLATION;
	private static final PasswordMinSizeViolation PASSWORD_MIN_SIZE_VIOLATION;
	private static final PasswordMaxSizeViolation PASSWORD_MAX_SIZE_VIOLATION;

	static {
		try (var in = PasswordConstraintFixture.class.getResourceAsStream(RESOURCE)) {
			if (in == null) {
				throw new IllegalStateException("Missing classpath resource: " + RESOURCE);
			}
			final var parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
			final var root = (JSONObject) parser.parse(new InputStreamReader(in, StandardCharsets.UTF_8));

			VALID = new ArrayList<>(toPasswordScenarios((JSONArray) root.get("valid")));
			VALID.add(new PasswordScenario(
					"a".repeat(Password.MAX_SIZE),
					"Senha com exatamente 128 caracteres (máximo) deveria ser aceita"));

			REJECTED_NULL = toPasswordScenarios((JSONArray) root.get("rejectedNull"));
			BELOW_MIN_SIZE = toPasswordScenarios((JSONArray) root.get("belowMinSize"));

			final var notNullBlock = (JSONObject) root.get("passwordNotNullViolation");
			final var notNullFailures = (JSONObject) notNullBlock.get("failureDescriptions");
			PASSWORD_NOT_NULL_VIOLATION = new PasswordNotNullViolation(
					(String) notNullBlock.get("sampleInvalidValue"),
					Objects.requireNonNull((String) notNullBlock.get("expectedMessagePtBr")),
					Objects.requireNonNull((String) notNullBlock.get("expectedPropertyPath")),
					Objects.requireNonNull((String) notNullFailures.get("violationCount")),
					Objects.requireNonNull((String) notNullFailures.get("constraintAnnotation")),
					Objects.requireNonNull((String) notNullFailures.get("message")),
					Objects.requireNonNull((String) notNullFailures.get("propertyPath")));

			final var minBlock = (JSONObject) root.get("passwordMinSizeViolation");
			PASSWORD_MIN_SIZE_VIOLATION = toPasswordMinSizeViolation(minBlock);
			assertPasswordMinSizeFixtureConsistent(PASSWORD_MIN_SIZE_VIOLATION);

			final var maxBlock = (JSONObject) root.get("passwordMaxSizeViolation");
			final var maxFailures = (JSONObject) maxBlock.get("failureDescriptions");
			final String tooLong = "a".repeat(Password.MAX_SIZE + 1);
			PASSWORD_MAX_SIZE_VIOLATION = new PasswordMaxSizeViolation(
					tooLong,
					Objects.requireNonNull((String) maxBlock.get("expectedMessagePtBr")),
					Objects.requireNonNull((String) maxBlock.get("expectedPropertyPath")),
					Objects.requireNonNull((String) maxFailures.get("violationCount")),
					Objects.requireNonNull((String) maxFailures.get("constraintAnnotation")),
					Objects.requireNonNull((String) maxFailures.get("message")),
					Objects.requireNonNull((String) maxFailures.get("propertyPath")));
			assertPasswordMaxSizeFixtureConsistent(PASSWORD_MAX_SIZE_VIOLATION);

			EXCEEDS_MAX_SIZE = List.of(new PasswordScenario(
					tooLong,
					"Senha com " + (Password.MAX_SIZE + 1) + " caracteres deveria violar @Size (máximo)"));
		} catch (final Exception e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	static Stream<Arguments> validPasswordArguments() {
		return VALID.stream().map(s -> Arguments.of(s.password(), s.failureDescription()));
	}

	static Stream<Arguments> rejectedNullArguments() {
		return REJECTED_NULL.stream().map(s -> Arguments.of(s.password(), s.failureDescription()));
	}

	static Stream<Arguments> belowMinSizeArguments() {
		return BELOW_MIN_SIZE.stream().map(s -> Arguments.of(s.password(), s.failureDescription()));
	}

	static Stream<Arguments> exceedsMaxSizeArguments() {
		return EXCEEDS_MAX_SIZE.stream().map(s -> Arguments.of(s.password(), s.failureDescription()));
	}

	static PasswordNotNullViolation passwordNotNullViolation() {
		return PASSWORD_NOT_NULL_VIOLATION;
	}

	static PasswordMinSizeViolation passwordMinSizeViolation() {
		return PASSWORD_MIN_SIZE_VIOLATION;
	}

	static PasswordMaxSizeViolation passwordMaxSizeViolation() {
		return PASSWORD_MAX_SIZE_VIOLATION;
	}

	private static PasswordMinSizeViolation toPasswordMinSizeViolation(final JSONObject obj) {
		final var failures = (JSONObject) obj.get("failureDescriptions");
		return new PasswordMinSizeViolation(
				Objects.requireNonNull((String) obj.get("sampleInvalidValue")),
				Objects.requireNonNull((String) obj.get("expectedMessagePtBr")),
				Objects.requireNonNull((String) obj.get("expectedPropertyPath")),
				Objects.requireNonNull((String) failures.get("violationCount")),
				Objects.requireNonNull((String) failures.get("constraintAnnotation")),
				Objects.requireNonNull((String) failures.get("message")),
				Objects.requireNonNull((String) failures.get("propertyPath")));
	}

	private static void assertPasswordMinSizeFixtureConsistent(final PasswordMinSizeViolation sample) {
		final int expectedLen = Password.MIN_SIZE - 1;
		if (sample.sampleInvalidValue().length() != expectedLen) {
			throw new IllegalStateException(
					"Fixture passwordMinSizeViolation.sampleInvalidValue deve ter " + expectedLen
							+ " caracteres (Password.MIN_SIZE - 1), tinha " + sample.sampleInvalidValue().length());
		}
		final String expectedMsg = "must contain between " + Password.MIN_SIZE + " and " + Password.MAX_SIZE
				+ " characters";
		if (!expectedMsg.equals(sample.expectedMessagePtBr())) {
			throw new IllegalStateException(
					"Fixture passwordMinSizeViolation.expectedMessagePtBr deve coincidir com a mensagem inglesa interpolada do bundle: esperado "
							+ expectedMsg + ", obtido " + sample.expectedMessagePtBr());
		}
	}

	private static void assertPasswordMaxSizeFixtureConsistent(final PasswordMaxSizeViolation sample) {
		final int expectedLen = Password.MAX_SIZE + 1;
		if (sample.sampleInvalidValue().length() != expectedLen) {
			throw new IllegalStateException(
					"Fixture passwordMaxSizeViolation.sampleInvalidValue deve ter " + expectedLen
							+ " caracteres (Password.MAX_SIZE + 1), tinha " + sample.sampleInvalidValue().length());
		}
		final String expectedMsg = "must contain between " + Password.MIN_SIZE + " and " + Password.MAX_SIZE
				+ " characters";
		if (!expectedMsg.equals(sample.expectedMessagePtBr())) {
			throw new IllegalStateException(
					"Fixture passwordMaxSizeViolation.expectedMessagePtBr deve coincidir com a mensagem inglesa interpolada do bundle: esperado "
							+ expectedMsg + ", obtido " + sample.expectedMessagePtBr());
		}
	}

	private static List<PasswordScenario> toPasswordScenarios(final JSONArray array) {
		return array.stream().map(PasswordConstraintFixture::toPasswordScenario).toList();
	}

	private static PasswordScenario toPasswordScenario(final Object element) {
		final var obj = (JSONObject) element;
		final Object rawPassword = obj.get("password");
		final String password = rawPassword == null ? null : (String) rawPassword;
		final String failureDescription = Objects.requireNonNull((String) obj.get("failureDescription"));
		return new PasswordScenario(password, failureDescription);
	}

	record PasswordScenario(String password, String failureDescription) {
	}

	record PasswordNotNullViolation(
			String sampleInvalidValue,
			String expectedMessagePtBr,
			String expectedPropertyPath,
			String failureDescriptionViolationCount,
			String failureDescriptionConstraintAnnotation,
			String failureDescriptionMessage,
			String failureDescriptionPropertyPath) {
	}

	record PasswordMinSizeViolation(
			String sampleInvalidValue,
			String expectedMessagePtBr,
			String expectedPropertyPath,
			String failureDescriptionViolationCount,
			String failureDescriptionConstraintAnnotation,
			String failureDescriptionMessage,
			String failureDescriptionPropertyPath) {
	}

	record PasswordMaxSizeViolation(
			String sampleInvalidValue,
			String expectedMessagePtBr,
			String expectedPropertyPath,
			String failureDescriptionViolationCount,
			String failureDescriptionConstraintAnnotation,
			String failureDescriptionMessage,
			String failureDescriptionPropertyPath) {
	}
}
