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

	private void assertPublicCheckerKeys(final JsonNode node) {
		assertThat(jsonObjectKeys(node)).containsExactlyInAnyOrder(
				"id", "profileId", "type", "replaces", "attempts", "updatedAt", "requiredPayload");
		assertThat(node.has("code")).isFalse();
		assertThat(node.has("payload")).isFalse();
	}

	private void assertCheckerNode(final JsonNode node, final CheckerJpaEntity expected) {
		assertPublicCheckerKeys(node);
		assertThat(node.get("id").asText()).isEqualTo(expected.getId().toString());
		assertThat(node.get("profileId").asText()).isEqualTo(expected.getProfileId().toString());
		assertThat(node.get("type").asText()).isEqualTo(expected.getType().name());
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
		@DisplayName("200 com visão pública de Alice CHANGE_EMAIL (sem code/payload)")
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
		@ValueSource(strings = { "CHANGE_PHONE", "VERIFY_PHONE", "2", "3" })
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
		@DisplayName("200 lista Alice ordenada por type, sem lastSeenType no JSON")
		void listsAliceOrderedByType() throws Exception {
			final MvcResult result = mockMvc.perform(get(Routes.CHECKER)
					.param("profileId", ALICE_ID.toString())
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode root = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(root)).containsExactlyInAnyOrder("limit", "content");
			assertThat(root.get("limit").asInt()).isEqualTo(100);
			assertThat(root.has("lastSeenType")).isFalse();
			final JsonNode content = root.get("content");
			assertThat(content.size()).isEqualTo(3);
			assertThat(content.get(0).get("id").asText()).isEqualTo(ALICE_CHANGE_EMAIL_ID.toString());
			assertThat(content.get(1).get("id").asText()).isEqualTo(ALICE_VERIFY_EMAIL_ID.toString());
			assertThat(content.get(2).get("id").asText()).isEqualTo(ALICE_CHANGE_PASSWORD_ID.toString());
			assertPublicCheckerKeys(content.get(0));
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
			assertThat(root.get("limit").asInt()).isEqualTo(10);
			assertThat(root.get("lastSeenType").asText()).isEqualTo("CHANGE_EMAIL");
			assertThat(root.get("content").size()).isEqualTo(2);
			assertThat(root.get("content").get(0).get("type").asText()).isEqualTo("VERIFY_EMAIL");
			assertThat(root.get("content").get(1).get("type").asText()).isEqualTo("CHANGE_PASSWORD");
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
		@DisplayName("POST CHANGE_EMAIL cria checker e não devolve code/payload")
		void postChangeEmailReturns200() throws Exception {
			final MvcResult result = mockMvc.perform(post(Routes.CHECKER)
					.param("profileId", CARLA_ID.toString())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "CHANGE_EMAIL" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertPublicCheckerKeys(n);
			assertThat(n.get("profileId").asText()).isEqualTo(CARLA_ID.toString());
			assertThat(n.get("type").asText()).isEqualTo("CHANGE_EMAIL");
			assertThat(n.get("attempts").asInt()).isEqualTo(10);
			assertThat(n.get("replaces").asInt()).isEqualTo(3);
			assertThat(n.get("requiredPayload").booleanValue()).isTrue();
			final var persisted = checkerRepository.findById(java.util.UUID.fromString(n.get("id").asText())).orElseThrow();
			assertThat(persisted.getCode()).matches("^[0-9]{6}$");
			assertThat(persisted.getPayload()).isNull();
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
		@ValueSource(strings = { "CHANGE_PHONE", "2", "3" })
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
		@DisplayName("POST com type numérico 2 retorna 400")
		void postNumericPhoneTypeReturns400() throws Exception {
			final var result = mockMvc.perform(post(Routes.CHECKER)
					.param("profileId", CARLA_ID.toString())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": 2 }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "type", "value not found", "2");
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
		@DisplayName("PUT omite campos e volta aos defaults")
		void putOmittedFieldsResetDefaults() throws Exception {
			final var before = checkerRepository.findById(BRUNO_CHANGE_EMAIL_ID).orElseThrow();
			assertThat(before.getPayload()).isNotNull();
			final MvcResult result = mockMvc.perform(put(Routes.CHECKER + "/" + BRUNO_CHANGE_EMAIL_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertPublicCheckerKeys(n);
			assertThat(n.get("attempts").asInt()).isEqualTo(10);
			assertThat(n.get("replaces").asInt()).isEqualTo(3);
			assertThat(n.get("requiredPayload").booleanValue()).isTrue();
			final var after = checkerRepository.findById(BRUNO_CHANGE_EMAIL_ID).orElseThrow();
			assertThat(after.getCode()).matches("^[0-9]{6}$");
			assertThat(after.getPayload()).isNull();
			assertThat(after.getAttempts()).isEqualTo((short) 10);
			assertThat(after.getReplaces()).isEqualTo((short) 3);
		}

		@Test
		@DisplayName("PUT aplica campos presentes")
		void putAppliesPresentFields() throws Exception {
			final MvcResult result = mockMvc.perform(put(Routes.CHECKER + "/" + BRUNO_CHANGE_EMAIL_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "code": "111222", "payload": "x", "attempts": 4, "replaces": 1 }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(n.get("attempts").asInt()).isEqualTo(4);
			assertThat(n.get("replaces").asInt()).isEqualTo(1);
			final var after = checkerRepository.findById(BRUNO_CHANGE_EMAIL_ID).orElseThrow();
			assertThat(after.getCode()).isEqualTo("111222");
			assertThat(after.getPayload()).isEqualTo("x");
		}

		@Test
		@DisplayName("PUT inexistente retorna 404")
		void putMissingReturns404() throws Exception {
			final var result = mockMvc.perform(put(Routes.CHECKER + "/" + UNKNOWN_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}

		@Test
		@DisplayName("PUT com code inválido retorna 400")
		void putInvalidCodeReturns400() throws Exception {
			final var result = mockMvc.perform(put(Routes.CHECKER + "/" + BRUNO_CHANGE_EMAIL_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "code": "12" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "code", "exactly 6");
		}

		@Test
		@DisplayName("PATCH altera só attempts")
		void patchOnlyAttempts() throws Exception {
			final var before = checkerRepository.findById(BRUNO_CHANGE_EMAIL_ID).orElseThrow();
			final MvcResult result = mockMvc.perform(patch(Routes.CHECKER + "/" + BRUNO_CHANGE_EMAIL_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "attempts": 1 }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(n.get("attempts").asInt()).isEqualTo(1);
			final var after = checkerRepository.findById(BRUNO_CHANGE_EMAIL_ID).orElseThrow();
			assertThat(after.getCode()).isEqualTo(before.getCode());
			assertThat(after.getPayload()).isEqualTo(before.getPayload());
			assertThat(after.getReplaces()).isEqualTo(before.getReplaces());
			assertThat(after.getAttempts()).isEqualTo((short) 1);
		}

		@Test
		@DisplayName("PATCH vazio devolve o estado atual")
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
			final var after = checkerRepository.findById(BRUNO_CHANGE_EMAIL_ID).orElseThrow();
			assertThat(after.getCode()).isEqualTo(before.getCode());
			assertThat(after.getPayload()).isEqualTo(before.getPayload());
		}

		@Test
		@DisplayName("PATCH inexistente retorna 404")
		void patchMissingReturns404() throws Exception {
			final var result = mockMvc.perform(patch(Routes.CHECKER + "/" + UNKNOWN_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "attempts": 1 }
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
		@DisplayName("DELETE VERIFY_EMAIL retorna 403")
		void deleteVerifyEmailReturns403() throws Exception {
			final MvcResult result = mockMvc.perform(delete(Routes.CHECKER + "/" + ALICE_VERIFY_EMAIL_ID))
					.andExpect(status().isForbidden())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("type");
			assertThat(n.get("type").get(0).asText()).contains("deleted");
			assertThat(checkerRepository.findById(ALICE_VERIFY_EMAIL_ID)).isPresent();
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
