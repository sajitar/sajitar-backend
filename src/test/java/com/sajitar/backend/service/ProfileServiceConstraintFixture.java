package com.sajitar.backend.service;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import com.sajitar.backend.domain.model.Profile;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;

/**
 * Carrega cenários de validação do {@link com.sajitar.backend.service.ProfileService}
 * a partir de {@code /fixtures/profile-service-validation.json}, no mesmo espírito
 * das fixtures de domínio ({@code name-validation.json}, {@code email-validation.json},
 * {@code limit-validation.json}).
 */
final class ProfileServiceConstraintFixture {

	private static final String RESOURCE = "/fixtures/profile-service-validation.json";

	private static final String VALID_CURSOR_NAME;
	private static final String VALID_SEARCH_NAME;
	private static final UUID VALID_UUID;
	private static final List<StringScenario> INVALID_NAME_PATTERN;
	private static final List<StringScenario> INVALID_EMAIL_FORMAT;
	private static final List<LimitScenario> INVALID_LIMIT_NOT_POSITIVE;
	private static final List<LimitScenario> INVALID_LIMIT_EXCEEDS_MAX;
	private static final List<StringScenario> BLANK_SEARCH_NAME;

	private static final ServiceConstraintSample FIND_BY_ID_NOT_NULL;
	private static final ServiceConstraintSample FIND_BY_EMAIL_NOT_NULL;
	private static final ServiceConstraintSample FIND_BY_EMAIL_JAKARTA;
	private static final ServiceConstraintSample FIND_ALL_TWO_LIMIT_NOT_NULL;
	private static final ServiceConstraintSample FIND_ALL_TWO_LIMIT_POSITIVE;
	private static final ServiceConstraintSample FIND_ALL_TWO_REVERSE_NOT_NULL;
	private static final ServiceConstraintSample FIND_ALL_FOUR_NAME_PATTERN;
	private static final ServiceConstraintSample FIND_ALL_FOUR_LAST_SEEN_ID_NOT_NULL;
	private static final ServiceConstraintSample FIND_BY_NAME_THREE_LIMIT_NOT_NULL;
	private static final ServiceConstraintSample FIND_BY_NAME_THREE_NOT_BLANK;
	private static final ServiceConstraintSample FIND_BY_NAME_FIVE_NAME_PATTERN;
	private static final ServiceConstraintSample COUNT_BY_NAME_NOT_BLANK;
	private static final ServiceConstraintSample COUNT_BY_NAME_FIVE_NAME_NOT_BLANK;
	private static final ServiceConstraintSample COUNT_BY_NAME_FIVE_REVERSE_NOT_NULL;
	private static final ServiceConstraintSample SAVE_PROFILE_NAME_PATTERN;
	private static final ServiceConstraintSample SAVE_PROFILE_EMAIL_NOT_NULL;
	private static final ServiceConstraintSample SAVE_PROFILE_PASSWORD_MIN_SIZE;

	static {
		try (var in = ProfileServiceConstraintFixture.class.getResourceAsStream(RESOURCE)) {
			if (in == null) {
				throw new IllegalStateException("Missing classpath resource: " + RESOURCE);
			}
			final var parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
			final var root = (JSONObject) parser.parse(new InputStreamReader(in, StandardCharsets.UTF_8));

			VALID_CURSOR_NAME = Objects.requireNonNull((String) root.get("validCursorName"));
			VALID_SEARCH_NAME = Objects.requireNonNull((String) root.get("validSearchName"));
			VALID_UUID = UUID.fromString(Objects.requireNonNull((String) root.get("validUuid")));

			INVALID_NAME_PATTERN = toStringScenarios((JSONArray) root.get("invalidNamePattern"));
			INVALID_EMAIL_FORMAT = toStringScenarios((JSONArray) root.get("invalidEmailFormat"));
			INVALID_LIMIT_NOT_POSITIVE = toLimitScenarios((JSONArray) root.get("invalidLimitNotPositive"));
			INVALID_LIMIT_EXCEEDS_MAX = toLimitScenarios((JSONArray) root.get("invalidLimitExceedsMax"));
			BLANK_SEARCH_NAME = toStringScenarios((JSONArray) root.get("blankSearchName"));

			FIND_BY_ID_NOT_NULL = toServiceSample((JSONObject) root.get("findByIdNotNullViolation"));
			FIND_BY_EMAIL_NOT_NULL = toServiceSample((JSONObject) root.get("findByEmailNotNullViolation"));
			FIND_BY_EMAIL_JAKARTA = toServiceSample((JSONObject) root.get("findByEmailJakartaViolation"));
			FIND_ALL_TWO_LIMIT_NOT_NULL = toServiceSample((JSONObject) root.get("findAllTwoArgsLimitNotNullViolation"));
			FIND_ALL_TWO_LIMIT_POSITIVE = toServiceSample((JSONObject) root.get("findAllTwoArgsLimitPositiveViolation"));
			FIND_ALL_TWO_REVERSE_NOT_NULL = toServiceSample((JSONObject) root.get("findAllTwoArgsReverseNotNullViolation"));
			FIND_ALL_FOUR_NAME_PATTERN = toServiceSample((JSONObject) root.get("findAllFourArgsNamePatternViolation"));
			FIND_ALL_FOUR_LAST_SEEN_ID_NOT_NULL = toServiceSample((JSONObject) root.get("findAllFourArgsLastSeenIdNotNullViolation"));
			FIND_BY_NAME_THREE_LIMIT_NOT_NULL = toServiceSample((JSONObject) root.get("findByNameThreeArgsLimitNotNullViolation"));
			FIND_BY_NAME_THREE_NOT_BLANK = toServiceSample((JSONObject) root.get("findByNameThreeArgsNotBlankViolation"));
			FIND_BY_NAME_FIVE_NAME_PATTERN = toServiceSample((JSONObject) root.get("findByNameFiveArgsNamePatternViolation"));
			COUNT_BY_NAME_NOT_BLANK = toServiceSample((JSONObject) root.get("countByNameNotBlankViolation"));
			COUNT_BY_NAME_FIVE_NAME_NOT_BLANK = toServiceSample((JSONObject) root.get("countByNameFiveArgsNameNotBlankViolation"));
			COUNT_BY_NAME_FIVE_REVERSE_NOT_NULL = toServiceSample((JSONObject) root.get("countByNameFiveArgsReverseNotNullViolation"));
			SAVE_PROFILE_NAME_PATTERN = toServiceSample((JSONObject) root.get("saveProfileNamePatternViolation"));
			SAVE_PROFILE_EMAIL_NOT_NULL = toServiceSample((JSONObject) root.get("saveProfileEmailNotNullViolation"));
			SAVE_PROFILE_PASSWORD_MIN_SIZE = toServiceSample((JSONObject) root.get("saveProfilePasswordMinSizeViolation"));
		} catch (final Exception e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private ProfileServiceConstraintFixture() {
	}

	static String validCursorName() {
		return VALID_CURSOR_NAME;
	}

	static String validSearchName() {
		return VALID_SEARCH_NAME;
	}

	static UUID validUuid() {
		return VALID_UUID;
	}

	static Stream<Arguments> invalidNamePatternArguments() {
		return INVALID_NAME_PATTERN.stream().map(s -> Arguments.of(s.value(), s.failureDescription()));
	}

	static Stream<Arguments> invalidEmailFormatArguments() {
		return INVALID_EMAIL_FORMAT.stream().map(s -> Arguments.of(s.value(), s.failureDescription()));
	}

	static Stream<Arguments> invalidLimitNotPositiveArguments() {
		return INVALID_LIMIT_NOT_POSITIVE.stream().map(s -> Arguments.of(s.limit(), s.failureDescription()));
	}

	static Stream<Arguments> invalidLimitExceedsMaxArguments() {
		return INVALID_LIMIT_EXCEEDS_MAX.stream().map(s -> Arguments.of(s.limit(), s.failureDescription()));
	}

	static Stream<Arguments> blankSearchNameArguments() {
		return BLANK_SEARCH_NAME.stream().map(s -> Arguments.of(s.value(), s.failureDescription()));
	}

	static ServiceConstraintSample findByIdNotNullViolation() {
		return FIND_BY_ID_NOT_NULL;
	}

	static ServiceConstraintSample findByEmailNotNullViolation() {
		return FIND_BY_EMAIL_NOT_NULL;
	}

	static ServiceConstraintSample findByEmailJakartaViolation() {
		return FIND_BY_EMAIL_JAKARTA;
	}

	static ServiceConstraintSample findAllTwoArgsLimitNotNullViolation() {
		return FIND_ALL_TWO_LIMIT_NOT_NULL;
	}

	static ServiceConstraintSample findAllTwoArgsLimitPositiveViolation() {
		return FIND_ALL_TWO_LIMIT_POSITIVE;
	}

	static ServiceConstraintSample findAllTwoArgsReverseNotNullViolation() {
		return FIND_ALL_TWO_REVERSE_NOT_NULL;
	}

	static ServiceConstraintSample findAllFourArgsNamePatternViolation() {
		return FIND_ALL_FOUR_NAME_PATTERN;
	}

	static ServiceConstraintSample findAllFourArgsLastSeenIdNotNullViolation() {
		return FIND_ALL_FOUR_LAST_SEEN_ID_NOT_NULL;
	}

	static ServiceConstraintSample findByNameThreeArgsLimitNotNullViolation() {
		return FIND_BY_NAME_THREE_LIMIT_NOT_NULL;
	}

	static ServiceConstraintSample findByNameThreeArgsNotBlankViolation() {
		return FIND_BY_NAME_THREE_NOT_BLANK;
	}

	static ServiceConstraintSample findByNameFiveArgsNamePatternViolation() {
		return FIND_BY_NAME_FIVE_NAME_PATTERN;
	}

	static ServiceConstraintSample countByNameNotBlankViolation() {
		return COUNT_BY_NAME_NOT_BLANK;
	}

	static ServiceConstraintSample countByNameFiveArgsNameNotBlankViolation() {
		return COUNT_BY_NAME_FIVE_NAME_NOT_BLANK;
	}

	static ServiceConstraintSample countByNameFiveArgsReverseNotNullViolation() {
		return COUNT_BY_NAME_FIVE_REVERSE_NOT_NULL;
	}

	static ServiceConstraintSample saveProfileNamePatternViolation() {
		return SAVE_PROFILE_NAME_PATTERN;
	}

	static ServiceConstraintSample saveProfileEmailNotNullViolation() {
		return SAVE_PROFILE_EMAIL_NOT_NULL;
	}

	static ServiceConstraintSample saveProfilePasswordMinSizeViolation() {
		return SAVE_PROFILE_PASSWORD_MIN_SIZE;
	}

	static Profile validProfile() {
		return Profile.builder()
				.id(VALID_UUID)
				.name(VALID_CURSOR_NAME)
				.email("user@example.com")
				.password("12345678")
				.birthday(LocalDate.parse("1988-01-10"))
				.description("Uma pessoa criativa e dedicada.")
				.build();
	}

	private static ServiceConstraintSample toServiceSample(final JSONObject jakarta) {
		final Object sampleInvalidValue = normalizeJsonSample(jakarta.get("sampleInvalidValue"));
		final var failures = (JSONObject) jakarta.get("failureDescriptions");
		return new ServiceConstraintSample(
				sampleInvalidValue,
				Objects.requireNonNull((String) jakarta.get("expectedMessagePtBr")),
				Objects.requireNonNull((String) jakarta.get("expectedPropertyPath")),
				Objects.requireNonNull((String) failures.get("violationCount")),
				Objects.requireNonNull((String) failures.get("constraintAnnotation")),
				Objects.requireNonNull((String) failures.get("message")),
				Objects.requireNonNull((String) failures.get("propertyPath")));
	}

	/**
	 * Valores em JSON podem vir como {@link String}, {@link Number} (ex.: limite 0) ou
	 * ausentes / {@code null}.
	 */
	private static Object normalizeJsonSample(final Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof final String s) {
			return s;
		}
		if (raw instanceof final Number n) {
			return n.intValue();
		}
		throw new IllegalArgumentException("sampleInvalidValue deve ser string, número ou null, obtido: " + raw.getClass());
	}

	private static List<StringScenario> toStringScenarios(final JSONArray array) {
		return array.stream().map(ProfileServiceConstraintFixture::toStringScenario).toList();
	}

	private static StringScenario toStringScenario(final Object element) {
		final var obj = (JSONObject) element;
		final String value;
		if (obj.containsKey("email")) {
			value = (String) obj.get("email");
		} else {
			value = (String) obj.get("name");
		}
		final String failureDescription = Objects.requireNonNull((String) obj.get("failureDescription"));
		return new StringScenario(value, failureDescription);
	}

	private static List<LimitScenario> toLimitScenarios(final JSONArray array) {
		return array.stream().map(ProfileServiceConstraintFixture::toLimitScenario).toList();
	}

	private static LimitScenario toLimitScenario(final Object element) {
		final var obj = (JSONObject) element;
		final int limit = ((Number) Objects.requireNonNull(obj.get("limit"))).intValue();
		final String failureDescription = Objects.requireNonNull((String) obj.get("failureDescription"));
		return new LimitScenario(limit, failureDescription);
	}

	record StringScenario(String value, String failureDescription) {
	}

	record LimitScenario(int limit, String failureDescription) {
	}

	record ServiceConstraintSample(
			Object sampleInvalidValue,
			String expectedMessagePtBr,
			String expectedPropertyPath,
			String failureDescriptionViolationCount,
			String failureDescriptionConstraintAnnotation,
			String failureDescriptionMessage,
			String failureDescriptionPropertyPath) {
	}
}
