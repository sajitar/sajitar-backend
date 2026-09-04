package com.sajitar.backend.controller;

import static com.sajitar.backend.settlement.checker.CheckerSettlementFixture.ALICE_CHANGE_EMAIL_ID;
import static com.sajitar.backend.settlement.checker.CheckerSettlementFixture.ALICE_CHANGE_PASSWORD_ID;
import static com.sajitar.backend.settlement.checker.CheckerSettlementFixture.ALICE_VERIFY_EMAIL_ID;
import static com.sajitar.backend.settlement.checker.CheckerSettlementFixture.BRUNO_CHANGE_EMAIL_ID;
import static com.sajitar.backend.settlement.checker.CheckerSettlementFixture.CARLA_ID;
import static com.sajitar.backend.settlement.profile.ProfileSettlementFixture.ALICE_ID;
import static com.sajitar.backend.settlement.profile.ProfileSettlementFixture.UNKNOWN_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sajitar.backend.adapter.in.web.checker.CheckerController;
import com.sajitar.backend.adapter.in.web.Routes;
import com.sajitar.backend.adapter.out.persistence.checker.CheckerJpaEntity;
import com.sajitar.backend.adapter.out.persistence.checker.CheckerJpaRepository;

/**
 * Integração do {@link CheckerController} com a massa
 * {@code classpath:settlement/checker.sql}.
 */
@SpringBootTest
@DisplayName("CheckerController (integração HTTP + settlement)")
class CheckerControllerIntegrationTest {

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private CheckerJpaRepository checkerRepository;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

	private static String responseBodyUtf8(final MvcResult result) {
		return new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
	}

	private Set<String> jsonObjectKeys(final JsonNode node) {
		final var keys = new HashSet<String>();
		node.fieldNames().forEachRemaining(keys::add);
		return keys;
	}

	private void assertNoContentBody(final MvcResult result) {
		assertThat(result.getResponse().getContentAsByteArray()).as("corpo 404").isEmpty();
	}

	private void assertCheckerKeys(final JsonNode node) {
		assertThat(jsonObjectKeys(node)).containsExactlyInAnyOrder(
				"id", "profileId", "type", "code", "payload", "replaces", "attempts", "updatedAt",
				"requiredPayload");
	}

	private void assertCheckerNode(final JsonNode node, final CheckerJpaEntity expected) {
		assertCheckerKeys(node);
		assertThat(node.get("id").asText()).isEqualTo(expected.getId().toString());
		assertThat(node.get("profileId").asText()).isEqualTo(expected.getProfileId().toString());
		assertThat(node.get("type").asText()).isEqualTo(expected.getType().name());
		assertThat(node.get("code").asText()).isEqualTo(expected.getCode());
		if (expected.getPayload() == null) {
			assertThat(node.get("payload").isNull()).isTrue();
		} else {
			assertThat(node.get("payload").asText()).isEqualTo(expected.getPayload());
		}
		assertThat(node.get("replaces").asInt()).isEqualTo(expected.getReplaces());
		assertThat(node.get("attempts").asInt()).isEqualTo(expected.getAttempts());
		assertThat(node.get("requiredPayload").isBoolean()).isTrue();
	}

	private void assertBadRequestSingleProperty(final MvcResult result, final String propertyKey,
			final String... messageSubstrings) throws Exception {
		assertThat(result.getResponse().getContentType()).as("Content-Type do 400").contains("json");
		final JsonNode root = objectMapper.readTree(responseBodyUtf8(result));
		assertThat(root.isObject()).isTrue();
		assertThat(jsonObjectKeys(root)).containsExactly(propertyKey);
		final JsonNode arr = root.get(propertyKey);
		assertThat(arr.isArray()).isTrue();
		assertThat(arr.size()).as("número de mensagens em %s", propertyKey).isEqualTo(1);
		final String text = arr.get(0).asText();
		for (final String part : messageSubstrings) {
			assertThat(text).as("mensagem de validação em %s", propertyKey).contains(part);
		}
	}

	@Nested
	@DisplayName("GET /checkers/{id}")
	class GetById {

		@Test
		@DisplayName("200 com Alice CHANGE_EMAIL incluindo code e payload nulo")
		void returns200WithAliceChangeEmail() throws Exception {
			final CheckerJpaEntity expected = checkerRepository.findById(ALICE_CHANGE_EMAIL_ID).orElseThrow();
			final MvcResult result = mockMvc.perform(get(Routes.CHECKER + "/" + ALICE_CHANGE_EMAIL_ID)
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertCheckerNode(n, expected);
			assertThat(n.get("requiredPayload").booleanValue()).isTrue();
		}

		@Test
		@DisplayName("200 com Bruno CHANGE_EMAIL incluindo code e payload")
		void returns200WithBrunoChangeEmail() throws Exception {
			final CheckerJpaEntity expected = checkerRepository.findById(BRUNO_CHANGE_EMAIL_ID).orElseThrow();
			final MvcResult result = mockMvc.perform(get(Routes.CHECKER + "/" + BRUNO_CHANGE_EMAIL_ID)
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertCheckerNode(n, expected);
			assertThat(n.get("code").asText()).isEqualTo("456789");
			assertThat(n.get("payload").asText()).isEqualTo("bruno-payload");
		}

		@Test
		@DisplayName("404 sem corpo quando o checker não existe")
		void returns404WhenMissing() throws Exception {
			final var result = mockMvc.perform(get(Routes.CHECKER + "/" + UNKNOWN_ID).accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}

		@Test
		@DisplayName("400 quando o id na URL não é um UUID válido")
		void returns400WhenIdIsNotUuid() throws Exception {
			mockMvc.perform(get(Routes.CHECKER + "/nao-e-uuid").accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest());
		}
	}

	@Nested
	@DisplayName("GET /checkers por profileId e type")
	class GetByProfileAndType {

		@Test
		@DisplayName("200 para o par Alice + VERIFY_EMAIL")
		void returns200ForPair() throws Exception {
			final CheckerJpaEntity expected = checkerRepository.findById(ALICE_VERIFY_EMAIL_ID).orElseThrow();
			final MvcResult result = mockMvc.perform(get(Routes.CHECKER)
					.param("profileId", ALICE_ID.toString())
					.param("type", "VERIFY_EMAIL")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertCheckerNode(n, expected);
			assertThat(n.get("requiredPayload").booleanValue()).isFalse();
		}

		@Test
		@DisplayName("404 sem corpo quando o par não existe")
		void returns404WhenPairMissing() throws Exception {
			final var result = mockMvc.perform(get(Routes.CHECKER)
					.param("profileId", CARLA_ID.toString())
					.param("type", "CHANGE_EMAIL")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}

		@Test
		@DisplayName("400 quando profileId falta na busca por type")
		void returns400WhenProfileIdMissing() throws Exception {
			final var result = mockMvc.perform(get(Routes.CHECKER)
					.param("type", "CHANGE_EMAIL")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "profileId", "must not be null");
		}

		@ParameterizedTest
		@ValueSource(strings = { "CHANGE_PHONE", "VERIFY_PHONE", "4", "3" })
		@DisplayName("400 para tipo de telefone ou valor desconhecido")
		void returns400ForPhoneType(final String type) throws Exception {
			final var result = mockMvc.perform(get(Routes.CHECKER)
					.param("profileId", ALICE_ID.toString())
					.param("type", type)
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "type", "value not found");
		}
	}

	@Nested
	@DisplayName("GET /checkers lista")
	class GetList {

		@Test
		@DisplayName("200 lista Alice ordenada por type, metadados de página")
		void listsAliceOrderedByType() throws Exception {
			final MvcResult result = mockMvc.perform(get(Routes.CHECKER)
					.param("profileId", ALICE_ID.toString())
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode root = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(root)).containsExactlyInAnyOrder(
					"content", "precedingElements", "followingElements", "reverse");
			assertThat(root.get("precedingElements").asLong()).isZero();
			assertThat(root.get("followingElements").asLong()).isZero();
			assertThat(root.get("reverse").booleanValue()).isFalse();
			final JsonNode content = root.get("content");
			assertThat(content.size()).isEqualTo(3);
			assertThat(content.get(0).get("id").asText()).isEqualTo(ALICE_CHANGE_EMAIL_ID.toString());
			assertThat(content.get(1).get("id").asText()).isEqualTo(ALICE_VERIFY_EMAIL_ID.toString());
			assertThat(content.get(2).get("id").asText()).isEqualTo(ALICE_CHANGE_PASSWORD_ID.toString());
			assertCheckerKeys(content.get(0));
		}

		@Test
		@DisplayName("200 com cursor lastSeenType=CHANGE_EMAIL")
		void listsAfterCursor() throws Exception {
			final MvcResult result = mockMvc.perform(get(Routes.CHECKER)
					.param("profileId", ALICE_ID.toString())
					.param("lastSeenType", "CHANGE_EMAIL")
					.param("limit", "10")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode root = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(root)).containsExactlyInAnyOrder(
					"content", "precedingElements", "followingElements", "reverse");
			assertThat(root.get("precedingElements").asLong()).isEqualTo(1);
			assertThat(root.get("followingElements").asLong()).isZero();
			assertThat(root.get("reverse").booleanValue()).isFalse();
			assertThat(root.get("content").size()).isEqualTo(2);
			assertThat(root.get("content").get(0).get("type").asText()).isEqualTo("VERIFY_EMAIL");
			assertThat(root.get("content").get(1).get("type").asText()).isEqualTo("CHANGE_PASSWORD");
		}

		@Test
		@DisplayName("200 com reverse=true ordena tipos descendentes")
		void listsAliceReverse() throws Exception {
			final MvcResult result = mockMvc.perform(get(Routes.CHECKER)
					.param("profileId", ALICE_ID.toString())
					.param("reverse", "true")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode root = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(root)).containsExactlyInAnyOrder(
					"content", "precedingElements", "followingElements", "reverse");
			assertThat(root.get("precedingElements").asLong()).isZero();
			assertThat(root.get("followingElements").asLong()).isZero();
			assertThat(root.get("reverse").booleanValue()).isTrue();
			final JsonNode content = root.get("content");
			assertThat(content.size()).isEqualTo(3);
			assertThat(content.get(0).get("type").asText()).isEqualTo("CHANGE_PASSWORD");
			assertThat(content.get(1).get("type").asText()).isEqualTo("VERIFY_EMAIL");
			assertThat(content.get(2).get("type").asText()).isEqualTo("CHANGE_EMAIL");
		}

		@Test
		@DisplayName("200 com reverse e cursor lastSeenType=CHANGE_PASSWORD")
		void listsAfterCursorReverse() throws Exception {
			final MvcResult result = mockMvc.perform(get(Routes.CHECKER)
					.param("profileId", ALICE_ID.toString())
					.param("lastSeenType", "CHANGE_PASSWORD")
					.param("reverse", "true")
					.param("limit", "10")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode root = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(root.get("precedingElements").asLong()).isEqualTo(1);
			assertThat(root.get("followingElements").asLong()).isZero();
			assertThat(root.get("reverse").booleanValue()).isTrue();
			assertThat(root.get("content").size()).isEqualTo(2);
			assertThat(root.get("content").get(0).get("type").asText()).isEqualTo("VERIFY_EMAIL");
			assertThat(root.get("content").get(1).get("type").asText()).isEqualTo("CHANGE_EMAIL");
		}

		@Test
		@DisplayName("404 quando a lista é vazia")
		void returns404WhenEmpty() throws Exception {
			final var result = mockMvc.perform(get(Routes.CHECKER)
					.param("profileId", CARLA_ID.toString())
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}

		@Test
		@DisplayName("404 quando o cursor não deixa itens")
		void returns404WhenCursorExhausted() throws Exception {
			final var result = mockMvc.perform(get(Routes.CHECKER)
					.param("profileId", ALICE_ID.toString())
					.param("lastSeenType", "CHANGE_PASSWORD")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}

		@Test
		@DisplayName("400 quando profileId falta na listagem")
		void returns400WhenProfileIdMissing() throws Exception {
			final var result = mockMvc.perform(get(Routes.CHECKER).accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "profileId", "must not be null");
		}

		@Test
		@DisplayName("400 quando lastSeenType é inválido")
		void returns400WhenLastSeenTypeIsInvalid() throws Exception {
			final var result = mockMvc.perform(get(Routes.CHECKER)
					.param("profileId", ALICE_ID.toString())
					.param("lastSeenType", "CHANGE_PHONE")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "type", "value not found");
		}
	}

	@Nested
	@Transactional
	@DisplayName("POST/PUT/PATCH/DELETE /checkers")
	class WriteCheckers {

		@Test
		@DisplayName("POST CHANGE_EMAIL cria checker e devolve code e payload")
		void postChangeEmailReturns200() throws Exception {
			final MvcResult result = mockMvc.perform(post(Routes.CHECKER)
					.param("profileId", CARLA_ID.toString())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "CHANGE_EMAIL", "payload": "carla-payload" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertCheckerKeys(n);
			assertThat(n.get("profileId").asText()).isEqualTo(CARLA_ID.toString());
			assertThat(n.get("type").asText()).isEqualTo("CHANGE_EMAIL");
			assertThat(n.get("attempts").asInt()).isEqualTo(10);
			assertThat(n.get("replaces").asInt()).isEqualTo(3);
			assertThat(n.get("requiredPayload").booleanValue()).isFalse();
			final var persisted = checkerRepository.findById(java.util.UUID.fromString(n.get("id").asText())).orElseThrow();
			assertThat(persisted.getCode()).matches("^[0-9]{6}$");
			assertThat(n.get("code").asText()).isEqualTo(persisted.getCode());
			assertThat(persisted.getPayload()).isEqualTo("carla-payload");
			assertThat(n.get("payload").asText()).isEqualTo("carla-payload");
		}

		@Test
		@DisplayName("POST CHANGE_PASSWORD cria checker")
		void postChangePasswordReturns200() throws Exception {
			final MvcResult result = mockMvc.perform(post(Routes.CHECKER)
					.param("profileId", CARLA_ID.toString())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "CHANGE_PASSWORD" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(n.get("type").asText()).isEqualTo("CHANGE_PASSWORD");
			assertThat(n.get("requiredPayload").booleanValue()).isTrue();
		}

		@Test
		@DisplayName("POST VERIFY_EMAIL retorna 403")
		void postVerifyEmailReturns403() throws Exception {
			final MvcResult result = mockMvc.perform(post(Routes.CHECKER)
					.param("profileId", CARLA_ID.toString())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "VERIFY_EMAIL" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isForbidden())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("type");
			assertThat(n.get("type").get(0).asText()).contains("internally");
		}

		@Test
		@DisplayName("POST duplicado retorna 409")
		void postDuplicateReturns409() throws Exception {
			final MvcResult result = mockMvc.perform(post(Routes.CHECKER)
					.param("profileId", ALICE_ID.toString())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "CHANGE_EMAIL" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isConflict())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("type");
			assertThat(n.get("type").get(0).asText()).contains("available type");
		}

		@Test
		@DisplayName("POST sem profileId retorna 400")
		void postWithoutProfileIdReturns400() throws Exception {
			final var result = mockMvc.perform(post(Routes.CHECKER)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "CHANGE_EMAIL" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "profileId", "must not be null");
		}

		@Test
		@DisplayName("POST com perfil inexistente retorna 404 com corpo")
		void postUnknownProfileReturns404() throws Exception {
			final var result = mockMvc.perform(post(Routes.CHECKER)
					.param("profileId", UNKNOWN_ID.toString())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "CHANGE_EMAIL" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("profileId");
			assertThat(n.get("profileId").get(0).asText()).contains("available");
		}

		@Test
		@DisplayName("POST com type nulo retorna 400")
		void postNullTypeReturns400() throws Exception {
			final var result = mockMvc.perform(post(Routes.CHECKER)
					.param("profileId", CARLA_ID.toString())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": null }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "type", "must not be null");
		}

		@ParameterizedTest
		@ValueSource(strings = { "CHANGE_PHONE", "4", "3" })
		@DisplayName("POST com tipo de telefone retorna 400")
		void postPhoneTypeReturns400(final String type) throws Exception {
			final var result = mockMvc.perform(post(Routes.CHECKER)
					.param("profileId", CARLA_ID.toString())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"type\": \"" + type + "\"}")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "type", "value not found");
		}

		@Test
		@DisplayName("POST com type numérico 4 retorna 400")
		void postNumericUnknownTypeReturns400() throws Exception {
			final var result = mockMvc.perform(post(Routes.CHECKER)
					.param("profileId", CARLA_ID.toString())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": 4 }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "type", "value not found", "4");
		}

		@ParameterizedTest(name = "lang={0}")
		@CsvSource({
				"pt, tipo disponível",
				"es, tipo disponible"
		})
		@DisplayName("POST duplicado respeita query lang")
		void postDuplicateRespectsLang(final String lang, final String expectedPart) throws Exception {
			final MvcResult result = mockMvc.perform(post(Routes.CHECKER)
					.param("profileId", ALICE_ID.toString())
					.param("lang", lang)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "CHANGE_EMAIL" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isConflict())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(n.get("type").get(0).asText()).contains(expectedPart);
		}

		@Test
		@DisplayName("PUT altera payload, gera código, attempts 10 e decrementa replaces")
		void putPayloadChangeConsumesReplace() throws Exception {
			final var before = checkerRepository.findById(BRUNO_CHANGE_EMAIL_ID).orElseThrow();
			assertThat(before.getReplaces()).isEqualTo((short) 2);
			final var previousCode = before.getCode();
			final MvcResult result = mockMvc.perform(put(Routes.CHECKER + "/" + BRUNO_CHANGE_EMAIL_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "CHANGE_EMAIL", "payload": "x" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertCheckerKeys(n);
			assertThat(n.get("id").asText()).isEqualTo(BRUNO_CHANGE_EMAIL_ID.toString());
			assertThat(n.get("profileId").asText()).isEqualTo(before.getProfileId().toString());
			assertThat(n.get("type").asText()).isEqualTo("CHANGE_EMAIL");
			assertThat(n.get("payload").asText()).isEqualTo("x");
			assertThat(n.get("attempts").asInt()).isEqualTo(10);
			assertThat(n.get("replaces").asInt()).isEqualTo(1);
			assertThat(n.get("code").asText()).matches("^[0-9]{6}$");
			assertThat(n.get("code").asText()).isNotEqualTo(previousCode);
			final var after = checkerRepository.findById(BRUNO_CHANGE_EMAIL_ID).orElseThrow();
			assertThat(after.getCode()).isEqualTo(n.get("code").asText());
			assertThat(after.getPayload()).isEqualTo("x");
			assertThat(after.getAttempts()).isEqualTo((short) 10);
			assertThat(after.getReplaces()).isEqualTo((short) 1);
		}

		@Test
		@DisplayName("PUT sem type retorna 400")
		void putWithoutTypeReturns400() throws Exception {
			final var result = mockMvc.perform(put(Routes.CHECKER + "/" + BRUNO_CHANGE_EMAIL_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "type", "must not be null");
		}

		@Test
		@DisplayName("PUT idêntico não consome replace")
		void putIdenticalDoesNotConsumeReplace() throws Exception {
			final var before = checkerRepository.findById(BRUNO_CHANGE_EMAIL_ID).orElseThrow();
			final MvcResult result = mockMvc.perform(put(Routes.CHECKER + "/" + BRUNO_CHANGE_EMAIL_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "CHANGE_EMAIL", "payload": "bruno-payload" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(n.get("code").asText()).isEqualTo(before.getCode());
			assertThat(n.get("replaces").asInt()).isEqualTo(before.getReplaces());
			assertThat(n.get("payload").asText()).isEqualTo("bruno-payload");
			final var after = checkerRepository.findById(BRUNO_CHANGE_EMAIL_ID).orElseThrow();
			assertThat(after.getCode()).isEqualTo(before.getCode());
			assertThat(after.getReplaces()).isEqualTo(before.getReplaces());
		}

		@Test
		@DisplayName("PUT para tipo já existente no perfil retorna 409")
		void putDuplicateTypeReturns409() throws Exception {
			final MvcResult result = mockMvc.perform(put(Routes.CHECKER + "/" + ALICE_CHANGE_EMAIL_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "CHANGE_PASSWORD" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isConflict())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("type");
			assertThat(n.get("type").get(0).asText()).contains("available type");
		}

		@Test
		@DisplayName("PUT para VERIFY_EMAIL retorna 403")
		void putVerifyEmailReturns403() throws Exception {
			final MvcResult result = mockMvc.perform(put(Routes.CHECKER + "/" + BRUNO_CHANGE_EMAIL_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "VERIFY_EMAIL" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isForbidden())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("type");
			assertThat(n.get("type").get(0).asText()).contains("internally");
		}

		@Test
		@DisplayName("PUT inexistente retorna 404")
		void putMissingReturns404() throws Exception {
			final var result = mockMvc.perform(put(Routes.CHECKER + "/" + UNKNOWN_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "CHANGE_EMAIL" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}

		@Test
		@DisplayName("PUT com replaces esgotado retorna 400")
		void putWhenReplacesExhaustedReturns400() throws Exception {
			mockMvc.perform(put(Routes.CHECKER + "/" + BRUNO_CHANGE_EMAIL_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "CHANGE_EMAIL", "payload": "a" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk());
			mockMvc.perform(put(Routes.CHECKER + "/" + BRUNO_CHANGE_EMAIL_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "CHANGE_EMAIL", "payload": "b" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk());
			final var result = mockMvc.perform(put(Routes.CHECKER + "/" + BRUNO_CHANGE_EMAIL_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "CHANGE_EMAIL", "payload": "c" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "replaces", "greater than 0");
			final var after = checkerRepository.findById(BRUNO_CHANGE_EMAIL_ID).orElseThrow();
			assertThat(after.getReplaces()).isEqualTo((short) 0);
			assertThat(after.getPayload()).isEqualTo("b");
		}

		@Test
		@DisplayName("PATCH altera payload e decrementa replaces")
		void patchPayloadConsumesReplace() throws Exception {
			final var before = checkerRepository.findById(BRUNO_CHANGE_EMAIL_ID).orElseThrow();
			final var previousCode = before.getCode();
			final var previousReplaces = before.getReplaces();
			final MvcResult result = mockMvc.perform(patch(Routes.CHECKER + "/" + BRUNO_CHANGE_EMAIL_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "payload": "novo" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(n.get("payload").asText()).isEqualTo("novo");
			assertThat(n.get("attempts").asInt()).isEqualTo(10);
			assertThat(n.get("replaces").asInt()).isEqualTo(previousReplaces - 1);
			assertThat(n.get("code").asText()).matches("^[0-9]{6}$");
			assertThat(n.get("code").asText()).isNotEqualTo(previousCode);
			assertThat(n.get("type").asText()).isEqualTo("CHANGE_EMAIL");
		}

		@Test
		@DisplayName("PATCH com payload nulo limpa a carga e decrementa replaces")
		void patchNullPayloadClears() throws Exception {
			final var before = checkerRepository.findById(BRUNO_CHANGE_EMAIL_ID).orElseThrow();
			final var previousCode = before.getCode();
			final var previousReplaces = before.getReplaces();
			final MvcResult result = mockMvc.perform(patch(Routes.CHECKER + "/" + BRUNO_CHANGE_EMAIL_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "payload": null }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(n.get("payload").isNull()).isTrue();
			assertThat(n.get("attempts").asInt()).isEqualTo(10);
			assertThat(n.get("replaces").asInt()).isEqualTo(previousReplaces - 1);
			assertThat(n.get("code").asText()).matches("^[0-9]{6}$");
			assertThat(n.get("code").asText()).isNotEqualTo(previousCode);
			assertThat(n.get("type").asText()).isEqualTo("CHANGE_EMAIL");
			final var after = checkerRepository.findById(BRUNO_CHANGE_EMAIL_ID).orElseThrow();
			assertThat(after.getPayload()).isNull();
		}

		@Test
		@DisplayName("PATCH vazio devolve o estado atual e não muda replaces")
		void emptyPatchReturnsCurrent() throws Exception {
			final var before = checkerRepository.findById(BRUNO_CHANGE_EMAIL_ID).orElseThrow();
			final MvcResult result = mockMvc.perform(patch(Routes.CHECKER + "/" + BRUNO_CHANGE_EMAIL_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(n.get("attempts").asInt()).isEqualTo(before.getAttempts());
			assertThat(n.get("replaces").asInt()).isEqualTo(before.getReplaces());
			assertThat(n.get("code").asText()).isEqualTo(before.getCode());
			final var after = checkerRepository.findById(BRUNO_CHANGE_EMAIL_ID).orElseThrow();
			assertThat(after.getCode()).isEqualTo(before.getCode());
			assertThat(after.getPayload()).isEqualTo(before.getPayload());
			assertThat(after.getReplaces()).isEqualTo(before.getReplaces());
		}

		@Test
		@DisplayName("PATCH para VERIFY_EMAIL retorna 403")
		void patchVerifyEmailReturns403() throws Exception {
			final MvcResult result = mockMvc.perform(patch(Routes.CHECKER + "/" + BRUNO_CHANGE_EMAIL_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "VERIFY_EMAIL" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isForbidden())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("type");
			assertThat(n.get("type").get(0).asText()).contains("internally");
		}

		@Test
		@DisplayName("PATCH com replaces esgotado retorna 400")
		void patchWhenReplacesExhaustedReturns400() throws Exception {
			mockMvc.perform(patch(Routes.CHECKER + "/" + BRUNO_CHANGE_EMAIL_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "payload": "a" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk());
			mockMvc.perform(patch(Routes.CHECKER + "/" + BRUNO_CHANGE_EMAIL_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "payload": "b" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk());
			final var result = mockMvc.perform(patch(Routes.CHECKER + "/" + BRUNO_CHANGE_EMAIL_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "payload": "c" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "replaces", "greater than 0");
		}

		@Test
		@DisplayName("PATCH inexistente retorna 404")
		void patchMissingReturns404() throws Exception {
			final var result = mockMvc.perform(patch(Routes.CHECKER + "/" + UNKNOWN_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "payload": "x" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}

		@Test
		@DisplayName("DELETE não restrito retorna 204")
		void deleteNonRestrictedReturns204() throws Exception {
			mockMvc.perform(delete(Routes.CHECKER + "/" + BRUNO_CHANGE_EMAIL_ID))
					.andExpect(status().isNoContent());
			assertThat(checkerRepository.findById(BRUNO_CHANGE_EMAIL_ID)).isEmpty();
		}

		@Test
		@DisplayName("DELETE VERIFY_EMAIL retorna 204")
		void deleteVerifyEmailReturns204() throws Exception {
			mockMvc.perform(delete(Routes.CHECKER + "/" + ALICE_VERIFY_EMAIL_ID))
					.andExpect(status().isNoContent());
			assertThat(checkerRepository.findById(ALICE_VERIFY_EMAIL_ID)).isEmpty();
		}

		@Test
		@DisplayName("DELETE inexistente retorna 404")
		void deleteMissingReturns404() throws Exception {
			final var result = mockMvc.perform(delete(Routes.CHECKER + "/" + UNKNOWN_ID))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}
	}

}
