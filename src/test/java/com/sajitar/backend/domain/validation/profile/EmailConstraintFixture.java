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
 * Carrega exemplos compartilhados de validação de e-mail a partir de
 * {@code /fixtures/email-validation.json}.
 */
final class EmailConstraintFixture {

	private static final String RESOURCE = "/fixtures/email-validation.json";

	private static final List<EmailScenario> VALID;
	private static final List<EmailScenario> INVALID_FORMAT;
	private static final List<EmailScenario> REJECTED_NULL_OR_BLANK;
	private static final List<EmailScenario> EXCEEDS_MAX_SIZE;
	private static final JakartaEmailViolation JAKARTA_EMAIL_VIOLATION;
	private static final EmailSizeViolation EMAIL_SIZE_VIOLATION;

	static {
		try (var in = EmailConstraintFixture.class.getResourceAsStream(RESOURCE)) {
			if (in == null) {
				throw new IllegalStateException("Missing classpath resource: " + RESOURCE);
			}
			final var parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
			final var root = (JSONObject) parser.parse(new InputStreamReader(in, StandardCharsets.UTF_8));

			VALID = toEmailScenarios((JSONArray) root.get("valid"));
			INVALID_FORMAT = toEmailScenarios((JSONArray) root.get("invalidFormat"));
			REJECTED_NULL_OR_BLANK = toEmailScenarios((JSONArray) root.get("rejectedNullOrBlank"));

			final var jakarta = (JSONObject) root.get("jakartaEmailViolation");
			final var jakartaFailures = (JSONObject) jakarta.get("failureDescriptions");
			JAKARTA_EMAIL_VIOLATION = new JakartaEmailViolation(
					Objects.requireNonNull((String) jakarta.get("sampleInvalidValue")),
					Objects.requireNonNull((String) jakarta.get("expectedMessagePtBr")),
					Objects.requireNonNull((String) jakarta.get("expectedPropertyPath")),
					Objects.requireNonNull((String) jakartaFailures.get("violationCount")),
					Objects.requireNonNull((String) jakartaFailures.get("constraintAnnotation")),
					Objects.requireNonNull((String) jakartaFailures.get("message")),
					Objects.requireNonNull((String) jakartaFailures.get("propertyPath")));

			final var sizeBlock = (JSONObject) root.get("emailSizeViolation");
			EMAIL_SIZE_VIOLATION = toEmailSizeViolation(sizeBlock);
			assertEmailSizeFixtureConsistent(EMAIL_SIZE_VIOLATION);
			EXCEEDS_MAX_SIZE = List.of(new EmailScenario(
					EMAIL_SIZE_VIOLATION.sampleInvalidValue(),
					"Endereço bem formado com " + (Email.MAX_SIZE + 1) + " caracteres deveria violar @Size"));
		} catch (final Exception e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	static Stream<Arguments> validEmailArguments() {
		return VALID.stream().map(s -> Arguments.of(s.email(), s.failureDescription()));
	}

	static Stream<Arguments> invalidFormatArguments() {
		return INVALID_FORMAT.stream().map(s -> Arguments.of(s.email(), s.failureDescription()));
	}

	static Stream<Arguments> rejectedNullOrBlankArguments() {
		return REJECTED_NULL_OR_BLANK.stream().map(s -> Arguments.of(s.email(), s.failureDescription()));
	}

	static Stream<Arguments> exceedsMaxSizeArguments() {
		return EXCEEDS_MAX_SIZE.stream().map(s -> Arguments.of(s.email(), s.failureDescription()));
	}

	static JakartaEmailViolation jakartaEmailViolation() {
		return JAKARTA_EMAIL_VIOLATION;
	}

	static EmailSizeViolation emailSizeViolation() {
		return EMAIL_SIZE_VIOLATION;
	}

	private static EmailSizeViolation toEmailSizeViolation(final JSONObject obj) {
		final var failures = (JSONObject) obj.get("failureDescriptions");
		return new EmailSizeViolation(
				Objects.requireNonNull((String) obj.get("sampleInvalidValue")),
				Objects.requireNonNull((String) obj.get("expectedMessagePtBr")),
				Objects.requireNonNull((String) obj.get("expectedPropertyPath")),
				Objects.requireNonNull((String) failures.get("violationCount")),
				Objects.requireNonNull((String) failures.get("constraintAnnotation")),
				Objects.requireNonNull((String) failures.get("message")),
				Objects.requireNonNull((String) failures.get("propertyPath")));
	}

	private static void assertEmailSizeFixtureConsistent(final EmailSizeViolation sample) {
		final int expectedLen = Email.MAX_SIZE + 1;
		if (sample.sampleInvalidValue().length() != expectedLen) {
			throw new IllegalStateException(
					"Fixture emailSizeViolation.sampleInvalidValue deve ter " + expectedLen + " caracteres (Email.MAX_SIZE + 1), tinha "
							+ sample.sampleInvalidValue().length());
		}
		final String expectedMsg = "must contain at most " + Email.MAX_SIZE + " characters";
		if (!expectedMsg.equals(sample.expectedMessagePtBr())) {
			throw new IllegalStateException(
					"Fixture emailSizeViolation.expectedMessagePtBr deve coincidir com a mensagem inglesa interpolada do bundle: esperado "
							+ expectedMsg + ", obtido " + sample.expectedMessagePtBr());
		}
	}

	private static List<EmailScenario> toEmailScenarios(final JSONArray array) {
		return array.stream().map(EmailConstraintFixture::toEmailScenario).toList();
	}

	private static EmailScenario toEmailScenario(final Object element) {
		final var obj = (JSONObject) element;
		final Object rawEmail = obj.get("email");
		final String email = rawEmail == null ? null : (String) rawEmail;
		final String failureDescription = Objects.requireNonNull((String) obj.get("failureDescription"));
		return new EmailScenario(email, failureDescription);
	}

	record EmailScenario(String email, String failureDescription) {
	}

	record JakartaEmailViolation(
			String sampleInvalidValue,
			String expectedMessagePtBr,
			String expectedPropertyPath,
			String failureDescriptionViolationCount,
			String failureDescriptionConstraintAnnotation,
			String failureDescriptionMessage,
			String failureDescriptionPropertyPath) {
	}

	record EmailSizeViolation(
			String sampleInvalidValue,
			String expectedMessagePtBr,
			String expectedPropertyPath,
			String failureDescriptionViolationCount,
			String failureDescriptionConstraintAnnotation,
			String failureDescriptionMessage,
			String failureDescriptionPropertyPath) {
	}
}
