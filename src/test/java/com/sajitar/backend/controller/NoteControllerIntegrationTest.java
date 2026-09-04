package com.sajitar.backend.controller;

import static com.sajitar.backend.settlement.note.NoteSettlementFixture.ALICE_PRIVATED_ID;
import static com.sajitar.backend.settlement.note.NoteSettlementFixture.ALICE_PROTECTED_ID;
import static com.sajitar.backend.settlement.note.NoteSettlementFixture.ALICE_PUBLIC_ONE_ID;
import static com.sajitar.backend.settlement.note.NoteSettlementFixture.ALICE_PUBLIC_TWO_ID;
import static com.sajitar.backend.settlement.note.NoteSettlementFixture.BRUNO_PUBLIC_ID;
import static com.sajitar.backend.settlement.note.NoteSettlementFixture.CARLA_ID;
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
import com.sajitar.backend.adapter.in.web.note.NoteController;
import com.sajitar.backend.adapter.out.persistence.note.NoteJpaEntity;
import com.sajitar.backend.adapter.out.persistence.note.NoteJpaRepository;
import com.sajitar.backend.domain.validation.note.Content;

/**
 * Integração do {@link NoteController} com a massa
 * {@code classpath:settlement/note.sql}.
 */
@SpringBootTest
@DisplayName("NoteController (integração HTTP + settlement)")
class NoteControllerIntegrationTest {

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private NoteJpaRepository noteRepository;

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

	private void assertNoteKeys(final JsonNode node) {
		assertThat(jsonObjectKeys(node)).containsExactlyInAnyOrder("id", "profileId", "type", "content");
	}

	private void assertNoteNode(final JsonNode node, final NoteJpaEntity expected) {
		assertNoteKeys(node);
		assertThat(node.get("id").asText()).isEqualTo(expected.getId().toString());
		assertThat(node.get("profileId").asText()).isEqualTo(expected.getProfileId().toString());
		assertThat(node.get("type").asText()).isEqualTo(expected.getType().name());
		assertThat(node.get("content").asText()).isEqualTo(expected.getContent());
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
	@DisplayName("GET /notes/{id}")
	class GetById {

		@Test
		@DisplayName("200 com Alice PUBLIC one")
		void returns200WithAlicePublicOne() throws Exception {
			final NoteJpaEntity expected = noteRepository.findById(ALICE_PUBLIC_ONE_ID).orElseThrow();
			final MvcResult result = mockMvc.perform(get(Routes.NOTE + "/" + ALICE_PUBLIC_ONE_ID)
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertNoteNode(n, expected);
		}

		@Test
		@DisplayName("200 com Bruno PUBLIC")
		void returns200WithBrunoPublic() throws Exception {
			final NoteJpaEntity expected = noteRepository.findById(BRUNO_PUBLIC_ID).orElseThrow();
			final MvcResult result = mockMvc.perform(get(Routes.NOTE + "/" + BRUNO_PUBLIC_ID)
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertNoteNode(n, expected);
			assertThat(n.get("type").asText()).isEqualTo("PUBLIC");
		}

		@Test
		@DisplayName("404 sem corpo quando a nota não existe")
		void returns404WhenMissing() throws Exception {
			final var result = mockMvc.perform(get(Routes.NOTE + "/" + UNKNOWN_ID).accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}

		@Test
		@DisplayName("400 quando o id na URL não é um UUID válido")
		void returns400WhenIdIsNotUuid() throws Exception {
			mockMvc.perform(get(Routes.NOTE + "/nao-e-uuid").accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest());
		}
	}

	@Nested
	@DisplayName("GET /notes lista")
	class GetList {

		@Test
		@DisplayName("200 lista Alice ordenada por id, metadados de página")
		void listsAliceOrderedById() throws Exception {
			final MvcResult result = mockMvc.perform(get(Routes.NOTE)
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
			assertThat(content.size()).isEqualTo(4);
			assertThat(content.get(0).get("id").asText()).isEqualTo(ALICE_PUBLIC_ONE_ID.toString());
			assertThat(content.get(1).get("id").asText()).isEqualTo(ALICE_PROTECTED_ID.toString());
			assertThat(content.get(2).get("id").asText()).isEqualTo(ALICE_PRIVATED_ID.toString());
			assertThat(content.get(3).get("id").asText()).isEqualTo(ALICE_PUBLIC_TWO_ID.toString());
			assertNoteKeys(content.get(0));
		}

		@Test
		@DisplayName("200 com cursor lastSeenId da primeira nota")
		void listsAfterCursor() throws Exception {
			final MvcResult result = mockMvc.perform(get(Routes.NOTE)
					.param("profileId", ALICE_ID.toString())
					.param("lastSeenId", ALICE_PUBLIC_ONE_ID.toString())
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
			assertThat(root.get("content").size()).isEqualTo(3);
			assertThat(root.get("content").get(0).get("id").asText()).isEqualTo(ALICE_PROTECTED_ID.toString());
			assertThat(root.get("content").get(1).get("id").asText()).isEqualTo(ALICE_PRIVATED_ID.toString());
			assertThat(root.get("content").get(2).get("id").asText()).isEqualTo(ALICE_PUBLIC_TWO_ID.toString());
		}

		@Test
		@DisplayName("200 com reverse=true ordena ids descendentes")
		void listsAliceReverse() throws Exception {
			final MvcResult result = mockMvc.perform(get(Routes.NOTE)
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
			assertThat(content.size()).isEqualTo(4);
			assertThat(content.get(0).get("id").asText()).isEqualTo(ALICE_PUBLIC_TWO_ID.toString());
			assertThat(content.get(1).get("id").asText()).isEqualTo(ALICE_PRIVATED_ID.toString());
			assertThat(content.get(2).get("id").asText()).isEqualTo(ALICE_PROTECTED_ID.toString());
			assertThat(content.get(3).get("id").asText()).isEqualTo(ALICE_PUBLIC_ONE_ID.toString());
		}

		@Test
		@DisplayName("200 com reverse e cursor lastSeenId da última nota")
		void listsAfterCursorReverse() throws Exception {
			final MvcResult result = mockMvc.perform(get(Routes.NOTE)
					.param("profileId", ALICE_ID.toString())
					.param("lastSeenId", ALICE_PUBLIC_TWO_ID.toString())
					.param("reverse", "true")
					.param("limit", "10")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode root = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(root.get("precedingElements").asLong()).isEqualTo(1);
			assertThat(root.get("followingElements").asLong()).isZero();
			assertThat(root.get("reverse").booleanValue()).isTrue();
			assertThat(root.get("content").size()).isEqualTo(3);
			assertThat(root.get("content").get(0).get("id").asText()).isEqualTo(ALICE_PRIVATED_ID.toString());
			assertThat(root.get("content").get(1).get("id").asText()).isEqualTo(ALICE_PROTECTED_ID.toString());
			assertThat(root.get("content").get(2).get("id").asText()).isEqualTo(ALICE_PUBLIC_ONE_ID.toString());
		}

		@Test
		@DisplayName("200 filtra type=PUBLIC e devolve página")
		void listsAlicePublicTypeFilter() throws Exception {
			final MvcResult result = mockMvc.perform(get(Routes.NOTE)
					.param("profileId", ALICE_ID.toString())
					.param("type", "PUBLIC")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode root = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(root)).containsExactlyInAnyOrder(
					"content", "precedingElements", "followingElements", "reverse");
			assertThat(root.get("content").size()).isEqualTo(2);
			assertThat(root.get("content").get(0).get("id").asText()).isEqualTo(ALICE_PUBLIC_ONE_ID.toString());
			assertThat(root.get("content").get(1).get("id").asText()).isEqualTo(ALICE_PUBLIC_TWO_ID.toString());
			assertThat(root.get("content").get(0).get("type").asText()).isEqualTo("PUBLIC");
			assertThat(root.get("content").get(1).get("type").asText()).isEqualTo("PUBLIC");
		}

		@Test
		@DisplayName("404 quando a lista é vazia")
		void returns404WhenEmpty() throws Exception {
			final var result = mockMvc.perform(get(Routes.NOTE)
					.param("profileId", CARLA_ID.toString())
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}

		@Test
		@DisplayName("404 quando o cursor não deixa itens")
		void returns404WhenCursorExhausted() throws Exception {
			final var result = mockMvc.perform(get(Routes.NOTE)
					.param("profileId", ALICE_ID.toString())
					.param("lastSeenId", ALICE_PUBLIC_TWO_ID.toString())
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}

		@Test
		@DisplayName("400 quando profileId falta na listagem")
		void returns400WhenProfileIdMissing() throws Exception {
			final var result = mockMvc.perform(get(Routes.NOTE).accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "profileId", "must not be null");
		}

		@Test
		@DisplayName("400 quando lastSeenId não é UUID")
		void returns400WhenLastSeenIdIsNotUuid() throws Exception {
			mockMvc.perform(get(Routes.NOTE)
					.param("profileId", ALICE_ID.toString())
					.param("lastSeenId", "nao-e-uuid")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest());
		}

		@ParameterizedTest
		@ValueSource(strings = { "SECRET", "HIDDEN", "4", "3" })
		@DisplayName("400 para tipo desconhecido no filtro")
		void returns400ForUnknownType(final String type) throws Exception {
			final var result = mockMvc.perform(get(Routes.NOTE)
					.param("profileId", ALICE_ID.toString())
					.param("type", type)
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "type", "value not found");
		}
	}

	@Nested
	@Transactional
	@DisplayName("POST/PUT/PATCH/DELETE /notes")
	class WriteNotes {

		@Test
		@DisplayName("POST PUBLIC cria nota")
		void postPublicReturns200() throws Exception {
			final MvcResult result = mockMvc.perform(post(Routes.NOTE)
					.param("profileId", CARLA_ID.toString())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "PUBLIC", "content": "Carla public" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertNoteKeys(n);
			assertThat(n.get("profileId").asText()).isEqualTo(CARLA_ID.toString());
			assertThat(n.get("type").asText()).isEqualTo("PUBLIC");
			assertThat(n.get("content").asText()).isEqualTo("Carla public");
			assertThat(n.get("id").asText()).isNotBlank();
		}

		@Test
		@DisplayName("POST segunda nota PUBLIC do mesmo perfil retorna 200")
		void postSecondPublicForSameProfileReturns200() throws Exception {
			mockMvc.perform(post(Routes.NOTE)
					.param("profileId", CARLA_ID.toString())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "PUBLIC", "content": "Carla public one" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk());
			final MvcResult result = mockMvc.perform(post(Routes.NOTE)
					.param("profileId", CARLA_ID.toString())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "PUBLIC", "content": "Carla public two" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(n.get("type").asText()).isEqualTo("PUBLIC");
			assertThat(n.get("content").asText()).isEqualTo("Carla public two");
		}

		@Test
		@DisplayName("POST sem profileId retorna 400")
		void postWithoutProfileIdReturns400() throws Exception {
			final var result = mockMvc.perform(post(Routes.NOTE)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "PUBLIC", "content": "x" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "profileId", "must not be null");
		}

		@Test
		@DisplayName("POST com perfil inexistente retorna 404 com corpo")
		void postUnknownProfileReturns404() throws Exception {
			final var result = mockMvc.perform(post(Routes.NOTE)
					.param("profileId", UNKNOWN_ID.toString())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "PUBLIC", "content": "x" }
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
			final var result = mockMvc.perform(post(Routes.NOTE)
					.param("profileId", CARLA_ID.toString())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": null, "content": "x" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "type", "must not be null");
		}

		@Test
		@DisplayName("POST com content em branco retorna 400")
		void postBlankContentReturns400() throws Exception {
			final var result = mockMvc.perform(post(Routes.NOTE)
					.param("profileId", CARLA_ID.toString())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "PUBLIC", "content": "" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "content", "must not be blank");
		}

		@Test
		@DisplayName("POST com content acima de 1000 caracteres retorna 400")
		void postTooLongContentReturns400() throws Exception {
			final var body = "{\"type\":\"PUBLIC\",\"content\":\"" + "a".repeat(Content.MAX_SIZE + 1) + "\"}";
			final var result = mockMvc.perform(post(Routes.NOTE)
					.param("profileId", CARLA_ID.toString())
					.contentType(MediaType.APPLICATION_JSON)
					.content(body)
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "content", "at most");
		}

		@ParameterizedTest
		@ValueSource(strings = { "SECRET", "4", "3" })
		@DisplayName("POST com tipo desconhecido retorna 400")
		void postUnknownTypeReturns400(final String type) throws Exception {
			final var result = mockMvc.perform(post(Routes.NOTE)
					.param("profileId", CARLA_ID.toString())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"type\": \"" + type + "\", \"content\": \"x\"}")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "type", "value not found");
		}

		@Test
		@DisplayName("POST com type numérico 4 retorna 400")
		void postNumericUnknownTypeReturns400() throws Exception {
			final var result = mockMvc.perform(post(Routes.NOTE)
					.param("profileId", CARLA_ID.toString())
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": 4, "content": "x" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "type", "value not found", "4");
		}

		@Test
		@DisplayName("PUT troca type e content e preserva id e profileId")
		void putTypeAndContentChange() throws Exception {
			final var before = noteRepository.findById(BRUNO_PUBLIC_ID).orElseThrow();
			final MvcResult result = mockMvc.perform(put(Routes.NOTE + "/" + BRUNO_PUBLIC_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "PRIVATE", "content": "Bruno privated" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertNoteKeys(n);
			assertThat(n.get("id").asText()).isEqualTo(BRUNO_PUBLIC_ID.toString());
			assertThat(n.get("profileId").asText()).isEqualTo(before.getProfileId().toString());
			assertThat(n.get("type").asText()).isEqualTo("PRIVATE");
			assertThat(n.get("content").asText()).isEqualTo("Bruno privated");
			final var after = noteRepository.findById(BRUNO_PUBLIC_ID).orElseThrow();
			assertThat(after.getType().name()).isEqualTo("PRIVATE");
			assertThat(after.getContent()).isEqualTo("Bruno privated");
		}

		@Test
		@DisplayName("PUT sem type retorna 400")
		void putWithoutTypeReturns400() throws Exception {
			final var result = mockMvc.perform(put(Routes.NOTE + "/" + BRUNO_PUBLIC_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "content": "x" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "type", "must not be null");
		}

		@Test
		@DisplayName("PUT idêntico não grava")
		void putIdenticalDoesNotPersist() throws Exception {
			final var before = noteRepository.findById(BRUNO_PUBLIC_ID).orElseThrow();
			final MvcResult result = mockMvc.perform(put(Routes.NOTE + "/" + BRUNO_PUBLIC_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "PUBLIC", "content": "Bruno public" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(n.get("type").asText()).isEqualTo("PUBLIC");
			assertThat(n.get("content").asText()).isEqualTo("Bruno public");
			final var after = noteRepository.findById(BRUNO_PUBLIC_ID).orElseThrow();
			assertThat(after.getType()).isEqualTo(before.getType());
			assertThat(after.getContent()).isEqualTo(before.getContent());
		}

		@Test
		@DisplayName("PUT inexistente retorna 404")
		void putMissingReturns404() throws Exception {
			final var result = mockMvc.perform(put(Routes.NOTE + "/" + UNKNOWN_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "PUBLIC", "content": "x" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}

		@Test
		@DisplayName("PATCH altera type")
		void patchTypeChange() throws Exception {
			final MvcResult result = mockMvc.perform(patch(Routes.NOTE + "/" + BRUNO_PUBLIC_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "PROTECTED" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(n.get("type").asText()).isEqualTo("PROTECTED");
			assertThat(n.get("content").asText()).isEqualTo("Bruno public");
			assertThat(n.get("id").asText()).isEqualTo(BRUNO_PUBLIC_ID.toString());
		}

		@Test
		@DisplayName("PATCH altera content")
		void patchContentChange() throws Exception {
			final MvcResult result = mockMvc.perform(patch(Routes.NOTE + "/" + BRUNO_PUBLIC_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "content": "Bruno atualizado" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(n.get("type").asText()).isEqualTo("PUBLIC");
			assertThat(n.get("content").asText()).isEqualTo("Bruno atualizado");
		}

		@Test
		@DisplayName("PATCH vazio devolve o estado atual")
		void patchEmptyKeepsState() throws Exception {
			final var before = noteRepository.findById(BRUNO_PUBLIC_ID).orElseThrow();
			final MvcResult result = mockMvc.perform(patch(Routes.NOTE + "/" + BRUNO_PUBLIC_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(n.get("type").asText()).isEqualTo(before.getType().name());
			assertThat(n.get("content").asText()).isEqualTo(before.getContent());
			final var after = noteRepository.findById(BRUNO_PUBLIC_ID).orElseThrow();
			assertThat(after.getType()).isEqualTo(before.getType());
			assertThat(after.getContent()).isEqualTo(before.getContent());
		}

		@Test
		@DisplayName("PATCH com content em branco retorna 400")
		void patchBlankContentReturns400() throws Exception {
			final var result = mockMvc.perform(patch(Routes.NOTE + "/" + BRUNO_PUBLIC_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "content": "" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "content", "must not be blank");
		}

		@Test
		@DisplayName("PATCH com content nulo retorna 400")
		void patchNullContentReturns400() throws Exception {
			final var result = mockMvc.perform(patch(Routes.NOTE + "/" + BRUNO_PUBLIC_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "content": null }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "content", "must not be blank");
		}

		@Test
		@DisplayName("PATCH com content acima de 1000 caracteres retorna 400")
		void patchTooLongContentReturns400() throws Exception {
			final var body = "{\"content\":\"" + "a".repeat(Content.MAX_SIZE + 1) + "\"}";
			final var result = mockMvc.perform(patch(Routes.NOTE + "/" + BRUNO_PUBLIC_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content(body)
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "content", "at most");
		}

		@Test
		@DisplayName("PATCH inexistente retorna 404")
		void patchMissingReturns404() throws Exception {
			final var result = mockMvc.perform(patch(Routes.NOTE + "/" + UNKNOWN_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "PUBLIC" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}

		@Test
		@DisplayName("DELETE retorna 204")
		void deleteReturns204() throws Exception {
			mockMvc.perform(delete(Routes.NOTE + "/" + BRUNO_PUBLIC_ID))
					.andExpect(status().isNoContent());
			assertThat(noteRepository.findById(BRUNO_PUBLIC_ID)).isEmpty();
		}

		@Test
		@DisplayName("DELETE PROTECTED retorna 204")
		void deleteProtectedReturns204() throws Exception {
			mockMvc.perform(delete(Routes.NOTE + "/" + ALICE_PROTECTED_ID))
					.andExpect(status().isNoContent());
			assertThat(noteRepository.findById(ALICE_PROTECTED_ID)).isEmpty();
		}

		@Test
		@DisplayName("DELETE inexistente retorna 404")
		void deleteMissingReturns404() throws Exception {
			final var result = mockMvc.perform(delete(Routes.NOTE + "/" + UNKNOWN_ID))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}
	}

}
