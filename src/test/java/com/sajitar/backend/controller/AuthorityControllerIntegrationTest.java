package com.sajitar.backend.controller;

import static com.sajitar.backend.settlement.authority.AuthoritySettlementFixture.ALICE_MASTER_ID;
import static com.sajitar.backend.settlement.authority.AuthoritySettlementFixture.ALICE_MEMBER_ID;
import static com.sajitar.backend.settlement.authority.AuthoritySettlementFixture.ALICE_READER_ID;
import static com.sajitar.backend.settlement.authority.AuthoritySettlementFixture.BRUNO_MASTER_ID;
import static com.sajitar.backend.settlement.authority.AuthoritySettlementFixture.CARLA_ID;
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
import com.sajitar.backend.adapter.in.web.Routes;
import com.sajitar.backend.adapter.in.web.authority.AuthorityController;
import com.sajitar.backend.adapter.out.persistence.authority.AuthorityJpaEntity;
import com.sajitar.backend.adapter.out.persistence.authority.AuthorityJpaRepository;

/**
 * Integração do {@link AuthorityController} com a massa
 * {@code classpath:settlement/authority.sql}.
 */
@SpringBootTest
@DisplayName("AuthorityController (integração HTTP + settlement)")
class AuthorityControllerIntegrationTest {

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private AuthorityJpaRepository authorityRepository;

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

	private void assertAuthorityKeys(final JsonNode node) {
		assertThat(jsonObjectKeys(node)).containsExactlyInAnyOrder("id", "profileId", "type");
	}

	private void assertAuthorityNode(final JsonNode node, final AuthorityJpaEntity expected) {
		assertAuthorityKeys(node);
		assertThat(node.get("id").asText()).isEqualTo(expected.getId().toString());
		assertThat(node.get("profileId").asText()).isEqualTo(expected.getProfileId().toString());
		assertThat(node.get("type").asText()).isEqualTo(expected.getType().name());
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
	@DisplayName("GET /authorities/{id}")
	class GetById {

		@Test
		@DisplayName("200 com Alice MASTER")
		void returns200WithAliceMaster() throws Exception {
			final AuthorityJpaEntity expected = authorityRepository.findById(ALICE_MASTER_ID).orElseThrow();
			final MvcResult result = mockMvc.perform(get(Routes.AUTHORITY + "/" + ALICE_MASTER_ID)
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertAuthorityNode(n, expected);
		}

		@Test
		@DisplayName("200 com Bruno MASTER")
		void returns200WithBrunoMaster() throws Exception {
			final AuthorityJpaEntity expected = authorityRepository.findById(BRUNO_MASTER_ID).orElseThrow();
			final MvcResult result = mockMvc.perform(get(Routes.AUTHORITY + "/" + BRUNO_MASTER_ID)
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertAuthorityNode(n, expected);
			assertThat(n.get("type").asText()).isEqualTo("MASTER");
		}

		@Test
		@DisplayName("404 sem corpo quando a authority não existe")
		void returns404WhenMissing() throws Exception {
			final var result = mockMvc.perform(get(Routes.AUTHORITY + "/" + UNKNOWN_ID).accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}

		@Test
		@DisplayName("400 quando o id na URL não é um UUID válido")
		void returns400WhenIdIsNotUuid() throws Exception {
			mockMvc.perform(get(Routes.AUTHORITY + "/nao-e-uuid").accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest());
		}
	}

	@Nested
	@DisplayName("GET /authorities por profileId e type")
	class GetByProfileAndType {

		@Test
		@DisplayName("200 para o par Alice + MEMBER")
		void returns200ForPair() throws Exception {
			final AuthorityJpaEntity expected = authorityRepository.findById(ALICE_MEMBER_ID).orElseThrow();
			final MvcResult result = mockMvc.perform(get(Routes.AUTHORITY)
					.param("profileId", ALICE_ID.toString())
					.param("type", "MEMBER")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertAuthorityNode(n, expected);
		}

		@Test
		@DisplayName("404 sem corpo quando o par não existe")
		void returns404WhenPairMissing() throws Exception {
			final var result = mockMvc.perform(get(Routes.AUTHORITY)
					.param("profileId", CARLA_ID.toString())
					.param("type", "MASTER")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}

		@Test
		@DisplayName("400 quando profileId falta na busca por type")
		void returns400WhenProfileIdMissing() throws Exception {
			final var result = mockMvc.perform(get(Routes.AUTHORITY)
					.param("type", "MASTER")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "profileId", "must not be null");
		}

		@ParameterizedTest
		@ValueSource(strings = { "ADMIN", "GUEST", "4", "3" })
		@DisplayName("400 para tipo desconhecido")
		void returns400ForUnknownType(final String type) throws Exception {
			final var result = mockMvc.perform(get(Routes.AUTHORITY)
					.param("profileId", ALICE_ID.toString())
					.param("type", type)
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "type", "value not found");
		}
	}

	@Nested
	@DisplayName("GET /authorities lista")
	class GetList {

		@Test
		@DisplayName("200 lista Alice ordenada por type, metadados de página")
		void listsAliceOrderedByType() throws Exception {
			final MvcResult result = mockMvc.perform(get(Routes.AUTHORITY)
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
			assertThat(content.get(0).get("id").asText()).isEqualTo(ALICE_MASTER_ID.toString());
			assertThat(content.get(1).get("id").asText()).isEqualTo(ALICE_MEMBER_ID.toString());
			assertThat(content.get(2).get("id").asText()).isEqualTo(ALICE_READER_ID.toString());
			assertAuthorityKeys(content.get(0));
		}

		@Test
		@DisplayName("200 com cursor lastSeenType=MASTER")
		void listsAfterCursor() throws Exception {
			final MvcResult result = mockMvc.perform(get(Routes.AUTHORITY)
					.param("profileId", ALICE_ID.toString())
					.param("lastSeenType", "MASTER")
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
			assertThat(root.get("content").get(0).get("type").asText()).isEqualTo("MEMBER");
			assertThat(root.get("content").get(1).get("type").asText()).isEqualTo("READER");
		}

		@Test
		@DisplayName("200 com reverse=true ordena tipos descendentes")
		void listsAliceReverse() throws Exception {
			final MvcResult result = mockMvc.perform(get(Routes.AUTHORITY)
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
			assertThat(content.get(0).get("type").asText()).isEqualTo("READER");
			assertThat(content.get(1).get("type").asText()).isEqualTo("MEMBER");
			assertThat(content.get(2).get("type").asText()).isEqualTo("MASTER");
		}

		@Test
		@DisplayName("200 com reverse e cursor lastSeenType=READER")
		void listsAfterCursorReverse() throws Exception {
			final MvcResult result = mockMvc.perform(get(Routes.AUTHORITY)
					.param("profileId", ALICE_ID.toString())
					.param("lastSeenType", "READER")
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
			assertThat(root.get("content").get(0).get("type").asText()).isEqualTo("MEMBER");
			assertThat(root.get("content").get(1).get("type").asText()).isEqualTo("MASTER");
		}

		@Test
		@DisplayName("404 quando a lista é vazia")
		void returns404WhenEmpty() throws Exception {
			final var result = mockMvc.perform(get(Routes.AUTHORITY)
					.param("profileId", CARLA_ID.toString())
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}

		@Test
		@DisplayName("404 quando o cursor não deixa itens")
		void returns404WhenCursorExhausted() throws Exception {
			final var result = mockMvc.perform(get(Routes.AUTHORITY)
					.param("profileId", ALICE_ID.toString())
					.param("lastSeenType", "READER")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}

		@Test
		@DisplayName("400 quando profileId falta na listagem")
		void returns400WhenProfileIdMissing() throws Exception {
			final var result = mockMvc.perform(get(Routes.AUTHORITY).accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "profileId", "must not be null");
		}

		@Test
		@DisplayName("400 quando lastSeenType é inválido")
		void returns400WhenLastSeenTypeIsInvalid() throws Exception {
			final var result = mockMvc.perform(get(Routes.AUTHORITY)
					.param("profileId", ALICE_ID.toString())
					.param("lastSeenType", "ADMIN")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "type", "value not found");
		}
	}

	@Nested
	@Transactional
	@DisplayName("POST/PUT/PATCH/DELETE /authorities")
	class WriteAuthorities {

		@Test
		@DisplayName("POST MASTER cria authority")
		void postMasterReturns200() throws Exception {
			final MvcResult result = mockMvc.perform(post(Routes.AUTHORITY)
					.param("profileId", CARLA_ID.toString())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "MASTER" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertAuthorityKeys(n);
			assertThat(n.get("profileId").asText()).isEqualTo(CARLA_ID.toString());
			assertThat(n.get("type").asText()).isEqualTo("MASTER");
			assertThat(n.get("id").asText()).isNotBlank();
		}

		@Test
		@DisplayName("POST MEMBER cria authority")
		void postMemberReturns200() throws Exception {
			final MvcResult result = mockMvc.perform(post(Routes.AUTHORITY)
					.param("profileId", CARLA_ID.toString())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "MEMBER" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(n.get("type").asText()).isEqualTo("MEMBER");
		}

		@Test
		@DisplayName("POST duplicado retorna 409")
		void postDuplicateReturns409() throws Exception {
			final MvcResult result = mockMvc.perform(post(Routes.AUTHORITY)
					.param("profileId", ALICE_ID.toString())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "MASTER" }
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
			final var result = mockMvc.perform(post(Routes.AUTHORITY)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "MASTER" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "profileId", "must not be null");
		}

		@Test
		@DisplayName("POST com perfil inexistente retorna 404 com corpo")
		void postUnknownProfileReturns404() throws Exception {
			final var result = mockMvc.perform(post(Routes.AUTHORITY)
					.param("profileId", UNKNOWN_ID.toString())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "MASTER" }
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
			final var result = mockMvc.perform(post(Routes.AUTHORITY)
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
		@ValueSource(strings = { "ADMIN", "4", "3" })
		@DisplayName("POST com tipo desconhecido retorna 400")
		void postUnknownTypeReturns400(final String type) throws Exception {
			final var result = mockMvc.perform(post(Routes.AUTHORITY)
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
			final var result = mockMvc.perform(post(Routes.AUTHORITY)
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
			final MvcResult result = mockMvc.perform(post(Routes.AUTHORITY)
					.param("profileId", ALICE_ID.toString())
					.param("lang", lang)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "MASTER" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isConflict())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(n.get("type").get(0).asText()).contains(expectedPart);
		}

		@Test
		@DisplayName("PUT troca type livre e preserva id e profileId")
		void putTypeChange() throws Exception {
			final var before = authorityRepository.findById(BRUNO_MASTER_ID).orElseThrow();
			final MvcResult result = mockMvc.perform(put(Routes.AUTHORITY + "/" + BRUNO_MASTER_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "READER" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertAuthorityKeys(n);
			assertThat(n.get("id").asText()).isEqualTo(BRUNO_MASTER_ID.toString());
			assertThat(n.get("profileId").asText()).isEqualTo(before.getProfileId().toString());
			assertThat(n.get("type").asText()).isEqualTo("READER");
			final var after = authorityRepository.findById(BRUNO_MASTER_ID).orElseThrow();
			assertThat(after.getType().name()).isEqualTo("READER");
		}

		@Test
		@DisplayName("PUT sem type retorna 400")
		void putWithoutTypeReturns400() throws Exception {
			final var result = mockMvc.perform(put(Routes.AUTHORITY + "/" + BRUNO_MASTER_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "type", "must not be null");
		}

		@Test
		@DisplayName("PUT idêntico não grava")
		void putIdenticalDoesNotPersist() throws Exception {
			final var before = authorityRepository.findById(BRUNO_MASTER_ID).orElseThrow();
			final MvcResult result = mockMvc.perform(put(Routes.AUTHORITY + "/" + BRUNO_MASTER_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "MASTER" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(n.get("type").asText()).isEqualTo("MASTER");
			final var after = authorityRepository.findById(BRUNO_MASTER_ID).orElseThrow();
			assertThat(after.getType()).isEqualTo(before.getType());
		}

		@Test
		@DisplayName("PUT para tipo já existente no perfil retorna 409")
		void putDuplicateTypeReturns409() throws Exception {
			final MvcResult result = mockMvc.perform(put(Routes.AUTHORITY + "/" + ALICE_MASTER_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "MEMBER" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isConflict())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("type");
		}

		@Test
		@DisplayName("PUT inexistente retorna 404")
		void putMissingReturns404() throws Exception {
			final var result = mockMvc.perform(put(Routes.AUTHORITY + "/" + UNKNOWN_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "MASTER" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}

		@Test
		@DisplayName("PATCH altera type")
		void patchTypeChange() throws Exception {
			final MvcResult result = mockMvc.perform(patch(Routes.AUTHORITY + "/" + BRUNO_MASTER_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "MEMBER" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(n.get("type").asText()).isEqualTo("MEMBER");
			assertThat(n.get("id").asText()).isEqualTo(BRUNO_MASTER_ID.toString());
		}

		@Test
		@DisplayName("PATCH vazio devolve o estado atual")
		void patchEmptyKeepsState() throws Exception {
			final var before = authorityRepository.findById(BRUNO_MASTER_ID).orElseThrow();
			final MvcResult result = mockMvc.perform(patch(Routes.AUTHORITY + "/" + BRUNO_MASTER_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(n.get("type").asText()).isEqualTo(before.getType().name());
			final var after = authorityRepository.findById(BRUNO_MASTER_ID).orElseThrow();
			assertThat(after.getType()).isEqualTo(before.getType());
		}

		@Test
		@DisplayName("PATCH para tipo já existente no perfil retorna 409")
		void patchDuplicateTypeReturns409() throws Exception {
			final MvcResult result = mockMvc.perform(patch(Routes.AUTHORITY + "/" + ALICE_MASTER_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "READER" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isConflict())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("type");
		}

		@Test
		@DisplayName("PATCH inexistente retorna 404")
		void patchMissingReturns404() throws Exception {
			final var result = mockMvc.perform(patch(Routes.AUTHORITY + "/" + UNKNOWN_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "MASTER" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}

		@Test
		@DisplayName("DELETE retorna 204")
		void deleteReturns204() throws Exception {
			mockMvc.perform(delete(Routes.AUTHORITY + "/" + BRUNO_MASTER_ID))
					.andExpect(status().isNoContent());
			assertThat(authorityRepository.findById(BRUNO_MASTER_ID)).isEmpty();
		}

		@Test
		@DisplayName("DELETE MEMBER retorna 204")
		void deleteMemberReturns204() throws Exception {
			mockMvc.perform(delete(Routes.AUTHORITY + "/" + ALICE_MEMBER_ID))
					.andExpect(status().isNoContent());
			assertThat(authorityRepository.findById(ALICE_MEMBER_ID)).isEmpty();
		}

		@Test
		@DisplayName("DELETE inexistente retorna 404")
		void deleteMissingReturns404() throws Exception {
			final var result = mockMvc.perform(delete(Routes.AUTHORITY + "/" + UNKNOWN_ID))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}
	}

}
