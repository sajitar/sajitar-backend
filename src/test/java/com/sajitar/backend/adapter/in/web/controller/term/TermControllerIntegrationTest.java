package com.sajitar.backend.adapter.in.web.controller.term;

import static com.sajitar.backend.settlement.profile.ProfileSettlementFixture.CARLA_ID;
import static com.sajitar.backend.settlement.term.TermSettlementFixture.DRAFT_ALL_ID;
import static com.sajitar.backend.settlement.term.TermSettlementFixture.DRAFT_EN_ID;
import static com.sajitar.backend.settlement.term.TermSettlementFixture.PUBLISHED_ID;
import static com.sajitar.backend.settlement.term.TermSettlementFixture.UNKNOWN_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sajitar.backend.adapter.in.web.Routes;
import com.sajitar.backend.adapter.in.web.controller.IntegrationAuth;
import com.sajitar.backend.adapter.out.persistence.term.TermJpaRepository;

/**
 * Integração do {@link TermController} com a massa
 * {@code classpath:settlement/term.sql}.
 */
@SpringBootTest
@DisplayName("TermController (integração HTTP + settlement)")
class TermControllerIntegrationTest {

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private TermJpaRepository termRepository;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = IntegrationAuth.withSecurityAndAliceBearer(webApplicationContext);
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
		assertThat(result.getResponse().getContentAsByteArray()).as("corpo sem conteúdo").isEmpty();
	}

	private void assertSingleProperty(final MvcResult result, final String propertyKey,
			final String... messageSubstrings) throws Exception {
		assertThat(result.getResponse().getContentType()).as("Content-Type do erro").contains("json");
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
	@DisplayName("GET /terms/current (público)")
	class GetCurrent {

		@Test
		@DisplayName("200 sem lang devolve id e contents sem draft, sem Bearer")
		void returns200WithoutLang() throws Exception {
			final MockMvc anonymous = IntegrationAuth.withSecurity(webApplicationContext);
			final MvcResult result = anonymous.perform(get(Routes.TERM + "/current")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andReturn();
			final JsonNode root = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(root)).containsExactlyInAnyOrder("id", "contents");
			assertThat(root.get("id").asText()).isEqualTo(PUBLISHED_ID.toString());
			final JsonNode contents = root.get("contents");
			assertThat(contents.size()).isEqualTo(3);
			assertThat(contents.get(0).get("type").asText()).isEqualTo("EN");
			assertThat(contents.get(1).get("type").asText()).isEqualTo("PT");
			assertThat(contents.get(2).get("type").asText()).isEqualTo("ES");
			assertThat(jsonObjectKeys(contents.get(0))).containsExactlyInAnyOrder("type", "content");
			assertThat(contents.get(0).has("draft")).isFalse();
		}

		@Test
		@DisplayName("200 com lang=pt devolve id, type e content sem draft")
		void returns200WithLang() throws Exception {
			final MockMvc anonymous = IntegrationAuth.withSecurity(webApplicationContext);
			final MvcResult result = anonymous.perform(get(Routes.TERM + "/current")
					.param("lang", "pt")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode root = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(root)).containsExactlyInAnyOrder("id", "type", "content");
			assertThat(root.get("id").asText()).isEqualTo(PUBLISHED_ID.toString());
			assertThat(root.get("type").asText()).isEqualTo("PT");
			assertThat(root.get("content").asText()).isEqualTo("Termos de uso (PT).");
			assertThat(root.has("draft")).isFalse();
		}

		@Test
		@DisplayName("400 lang com a chave lang quando a língua é desconhecida")
		void returns400ForUnknownLang() throws Exception {
			final MockMvc anonymous = IntegrationAuth.withSecurity(webApplicationContext);
			final MvcResult result = anonymous.perform(get(Routes.TERM + "/current")
					.param("lang", "fr")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertSingleProperty(result, "lang", "value not found", "fr");
		}
	}

	@Nested
	@Transactional
	@DisplayName("GET /terms/current sem vigente")
	class GetCurrentEmpty {

		@Test
		@DisplayName("404 sem corpo quando não há id com as três línguas publicadas")
		void returns404WhenNoCurrent() throws Exception {
			termRepository.deleteAll();
			final MockMvc anonymous = IntegrationAuth.withSecurity(webApplicationContext);
			final MvcResult result = anonymous.perform(get(Routes.TERM + "/current")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}
	}

	@Nested
	@DisplayName("GET /terms/{id}")
	class GetById {

		@Test
		@DisplayName("200 sem lang devolve contents com draft para o id publicado")
		void returns200WithoutLang() throws Exception {
			final MvcResult result = mockMvc.perform(get(Routes.TERM + "/" + PUBLISHED_ID)
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode root = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(root)).containsExactlyInAnyOrder("id", "contents");
			assertThat(root.get("id").asText()).isEqualTo(PUBLISHED_ID.toString());
			final JsonNode contents = root.get("contents");
			assertThat(contents.size()).isEqualTo(3);
			assertThat(jsonObjectKeys(contents.get(0))).containsExactlyInAnyOrder("type", "content", "draft");
			assertThat(contents.get(0).get("type").asText()).isEqualTo("EN");
			assertThat(contents.get(0).get("draft").booleanValue()).isFalse();
		}

		@Test
		@DisplayName("200 com lang=en devolve id, type, content e draft")
		void returns200WithLang() throws Exception {
			final MvcResult result = mockMvc.perform(get(Routes.TERM + "/" + PUBLISHED_ID)
					.param("lang", "en")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode root = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(root)).containsExactlyInAnyOrder("id", "type", "content", "draft");
			assertThat(root.get("type").asText()).isEqualTo("EN");
			assertThat(root.get("content").asText()).isEqualTo("Terms of use (EN).");
			assertThat(root.get("draft").booleanValue()).isFalse();
		}

		@Test
		@DisplayName("200 devolve contents com uma só linha em draft para o id incompleto")
		void returns200ForDraftOnly() throws Exception {
			final MvcResult result = mockMvc.perform(get(Routes.TERM + "/" + DRAFT_EN_ID)
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode root = objectMapper.readTree(responseBodyUtf8(result));
			final JsonNode contents = root.get("contents");
			assertThat(contents.size()).isEqualTo(1);
			assertThat(contents.get(0).get("type").asText()).isEqualTo("EN");
			assertThat(contents.get(0).get("draft").booleanValue()).isTrue();
		}

		@Test
		@DisplayName("400 type quando a língua pedida não existe no id")
		void returns400WhenTypeAbsent() throws Exception {
			final MvcResult result = mockMvc.perform(get(Routes.TERM + "/" + DRAFT_EN_ID)
					.param("lang", "pt")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertSingleProperty(result, "type", "gravado neste termo");
		}

		@Test
		@DisplayName("400 lang quando a língua pedida é desconhecida")
		void returns400ForUnknownLang() throws Exception {
			final MvcResult result = mockMvc.perform(get(Routes.TERM + "/" + PUBLISHED_ID)
					.param("lang", "xx")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertSingleProperty(result, "lang", "value not found", "xx");
		}

		@Test
		@DisplayName("404 sem corpo quando o id não existe")
		void returns404WhenMissing() throws Exception {
			final MvcResult result = mockMvc.perform(get(Routes.TERM + "/" + UNKNOWN_ID)
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}

		@Test
		@DisplayName("400 quando o id na URL não é um UUID válido")
		void returns400WhenIdIsNotUuid() throws Exception {
			mockMvc.perform(get(Routes.TERM + "/nao-e-uuid").accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest());
		}
	}

	@Nested
	@DisplayName("GET /terms lista")
	class GetList {

		@Test
		@DisplayName("200 sem lang lista os ids ascendentes com contents")
		void listsWithoutLang() throws Exception {
			final MvcResult result = mockMvc.perform(get(Routes.TERM)
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
			assertThat(content.get(0).get("id").asText()).isEqualTo(PUBLISHED_ID.toString());
			assertThat(content.get(1).get("id").asText()).isEqualTo(DRAFT_ALL_ID.toString());
			assertThat(content.get(2).get("id").asText()).isEqualTo(DRAFT_EN_ID.toString());
			assertThat(jsonObjectKeys(content.get(0))).containsExactlyInAnyOrder("id", "contents");
			assertThat(content.get(0).get("contents").size()).isEqualTo(3);
			assertThat(content.get(2).get("contents").size()).isEqualTo(1);
		}

		@Test
		@DisplayName("200 com lang=en traz uma linha por id para os três ids")
		void listsWithEnglish() throws Exception {
			final MvcResult result = mockMvc.perform(get(Routes.TERM)
					.param("lang", "en")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode content = objectMapper.readTree(responseBodyUtf8(result)).get("content");
			assertThat(content.size()).isEqualTo(3);
			assertThat(jsonObjectKeys(content.get(0))).containsExactlyInAnyOrder("id", "type", "content", "draft");
			assertThat(content.get(0).get("type").asText()).isEqualTo("EN");
			assertThat(content.get(0).get("draft").booleanValue()).isFalse();
			assertThat(content.get(2).get("draft").booleanValue()).isTrue();
		}

		@Test
		@DisplayName("200 com lang=pt omite o id que não tem a língua")
		void listsWithPortuguese() throws Exception {
			final MvcResult result = mockMvc.perform(get(Routes.TERM)
					.param("lang", "pt")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode content = objectMapper.readTree(responseBodyUtf8(result)).get("content");
			assertThat(content.size()).isEqualTo(2);
			assertThat(content.get(0).get("id").asText()).isEqualTo(PUBLISHED_ID.toString());
			assertThat(content.get(1).get("id").asText()).isEqualTo(DRAFT_ALL_ID.toString());
			assertThat(content.get(0).get("type").asText()).isEqualTo("PT");
		}

		@Test
		@DisplayName("200 com reverse=true ordena os ids descendentes")
		void listsReverse() throws Exception {
			final MvcResult result = mockMvc.perform(get(Routes.TERM)
					.param("reverse", "true")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode root = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(root.get("reverse").booleanValue()).isTrue();
			final JsonNode content = root.get("content");
			assertThat(content.size()).isEqualTo(3);
			assertThat(content.get(0).get("id").asText()).isEqualTo(DRAFT_EN_ID.toString());
			assertThat(content.get(1).get("id").asText()).isEqualTo(DRAFT_ALL_ID.toString());
			assertThat(content.get(2).get("id").asText()).isEqualTo(PUBLISHED_ID.toString());
		}

		@Test
		@DisplayName("200 com cursor lastSeenId do primeiro id")
		void listsAfterCursor() throws Exception {
			final MvcResult result = mockMvc.perform(get(Routes.TERM)
					.param("lastSeenId", PUBLISHED_ID.toString())
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode root = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(root.get("precedingElements").asLong()).isEqualTo(1);
			assertThat(root.get("followingElements").asLong()).isZero();
			final JsonNode content = root.get("content");
			assertThat(content.size()).isEqualTo(2);
			assertThat(content.get(0).get("id").asText()).isEqualTo(DRAFT_ALL_ID.toString());
			assertThat(content.get(1).get("id").asText()).isEqualTo(DRAFT_EN_ID.toString());
		}

		@Test
		@DisplayName("400 lang quando a língua do filtro é desconhecida")
		void returns400ForUnknownLang() throws Exception {
			final MvcResult result = mockMvc.perform(get(Routes.TERM)
					.param("lang", "xx")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertSingleProperty(result, "lang", "value not found", "xx");
		}

		@Test
		@DisplayName("404 sem corpo quando o cursor não deixa itens")
		void returns404WhenCursorExhausted() throws Exception {
			final MvcResult result = mockMvc.perform(get(Routes.TERM)
					.param("lastSeenId", DRAFT_EN_ID.toString())
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}
	}

	@Nested
	@DisplayName("Acesso às rotas protegidas")
	class Access {

		@Test
		@DisplayName("401 token em GET /terms sem Bearer")
		void listWithoutBearerReturns401() throws Exception {
			final MockMvc anonymous = IntegrationAuth.withSecurity(webApplicationContext);
			final MvcResult result = anonymous.perform(get(Routes.TERM).accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isUnauthorized())
					.andReturn();
			final JsonNode root = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(root)).containsExactly("token");
			assertThat(root.get("token").get(0).asText()).contains("bearer token");
		}

		@Test
		@DisplayName("401 token em POST /terms sem Bearer")
		void postWithoutBearerReturns401() throws Exception {
			final MockMvc anonymous = IntegrationAuth.withSecurity(webApplicationContext);
			anonymous.perform(post(Routes.TERM)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "EN", "content": "x" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isUnauthorized());
		}

		@Test
		@DisplayName("403 id quando o perfil autenticado não inclui MASTER (GET)")
		void listWithoutMasterReturns403() throws Exception {
			final MockMvc reader = IntegrationAuth.withSecurityAndBearer(webApplicationContext, CARLA_ID);
			final MvcResult result = reader.perform(get(Routes.TERM).accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isForbidden())
					.andReturn();
			assertSingleProperty(result, "id", "requested by a master");
		}

		@Test
		@DisplayName("403 id quando o perfil autenticado não inclui MASTER (POST)")
		void postWithoutMasterReturns403() throws Exception {
			final MockMvc reader = IntegrationAuth.withSecurityAndBearer(webApplicationContext, CARLA_ID);
			final MvcResult result = reader.perform(post(Routes.TERM)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "EN", "content": "x" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isForbidden())
					.andReturn();
			assertSingleProperty(result, "id", "requested by a master");
		}
	}

	@Nested
	@Transactional
	@DisplayName("POST/PATCH/DELETE /terms")
	class Write {

		@Test
		@DisplayName("POST cria a primeira linha com draft true e id gerado")
		void postCreatesDraftLine() throws Exception {
			final MvcResult result = mockMvc.perform(post(Routes.TERM)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "EN", "content": "Brand new EN line." }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode root = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(root)).containsExactlyInAnyOrder("id", "type", "content", "draft");
			assertThat(root.get("id").asText()).isNotBlank();
			assertThat(root.get("type").asText()).isEqualTo("EN");
			assertThat(root.get("content").asText()).isEqualTo("Brand new EN line.");
			assertThat(root.get("draft").booleanValue()).isTrue();
		}

		@Test
		@DisplayName("POST sem type retorna 400 type")
		void postWithoutTypeReturns400() throws Exception {
			final MvcResult result = mockMvc.perform(post(Routes.TERM)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "content": "x" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertSingleProperty(result, "type", "must not be null");
		}

		@Test
		@DisplayName("POST com content em branco retorna 400 content")
		void postBlankContentReturns400() throws Exception {
			final MvcResult result = mockMvc.perform(post(Routes.TERM)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "EN", "content": "" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertSingleProperty(result, "content", "must not be blank");
		}

		@ParameterizedTest
		@ValueSource(strings = { "XX", "FR", "4" })
		@DisplayName("POST com type desconhecido retorna 400 type")
		void postUnknownTypeReturns400(final String type) throws Exception {
			final MvcResult result = mockMvc.perform(post(Routes.TERM)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"type\": \"" + type + "\", \"content\": \"x\"}")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertSingleProperty(result, "type", "value not found");
		}

		@Test
		@DisplayName("POST /{id} acrescenta uma língua ao id existente com draft true")
		void postTypeAddsLine() throws Exception {
			final MvcResult result = mockMvc.perform(post(Routes.TERM + "/" + DRAFT_EN_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "PT", "content": "Nova linha PT." }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode root = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(root.get("id").asText()).isEqualTo(DRAFT_EN_ID.toString());
			assertThat(root.get("type").asText()).isEqualTo("PT");
			assertThat(root.get("content").asText()).isEqualTo("Nova linha PT.");
			assertThat(root.get("draft").booleanValue()).isTrue();
			assertThat(termRepository.findLinesById(DRAFT_EN_ID)).hasSize(2);
		}

		@Test
		@DisplayName("POST /{id} com a língua já gravada retorna 409 sem corpo")
		void postDuplicateTypeReturns409() throws Exception {
			final MvcResult result = mockMvc.perform(post(Routes.TERM + "/" + DRAFT_EN_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "EN", "content": "x" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isConflict())
					.andReturn();
			assertNoContentBody(result);
		}

		@Test
		@DisplayName("POST /{id} com id inexistente retorna 404 sem corpo")
		void postTypeMissingIdReturns404() throws Exception {
			final MvcResult result = mockMvc.perform(post(Routes.TERM + "/" + UNKNOWN_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "EN", "content": "x" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}

		@Test
		@DisplayName("PATCH altera o content e mantém draft true quando draft é omitido")
		void patchKeepsDraft() throws Exception {
			final MvcResult result = mockMvc.perform(patch(Routes.TERM + "/" + DRAFT_EN_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "EN", "content": "Only EN draft, edited." }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode root = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(root.get("type").asText()).isEqualTo("EN");
			assertThat(root.get("content").asText()).isEqualTo("Only EN draft, edited.");
			assertThat(root.get("draft").booleanValue()).isTrue();
		}

		@Test
		@DisplayName("PATCH com draft false publica a linha quando o id tem EN, PT e ES")
		void patchPublishesLine() throws Exception {
			final MvcResult result = mockMvc.perform(patch(Routes.TERM + "/" + DRAFT_ALL_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "EN", "content": "Draft terms (EN), published.", "draft": false }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode root = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(root.get("type").asText()).isEqualTo("EN");
			assertThat(root.get("draft").booleanValue()).isFalse();
		}

		@Test
		@DisplayName("PATCH com draft false sem as três línguas retorna 400 draft")
		void patchIncompletePublishReturns400() throws Exception {
			final MvcResult result = mockMvc.perform(patch(Routes.TERM + "/" + DRAFT_EN_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "EN", "content": "Only EN draft.", "draft": false }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertSingleProperty(result, "draft", "EN, PT and ES");
		}

		@Test
		@DisplayName("PATCH de uma linha já publicada retorna 400 draft")
		void patchClosedLineReturns400() throws Exception {
			final MvcResult result = mockMvc.perform(patch(Routes.TERM + "/" + PUBLISHED_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "EN", "content": "x", "draft": false }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertSingleProperty(result, "draft", "open draft");
		}

		@Test
		@DisplayName("PATCH com draft null retorna 400 draft")
		void patchNullDraftReturns400() throws Exception {
			final MvcResult result = mockMvc.perform(patch(Routes.TERM + "/" + DRAFT_EN_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "EN", "content": "x", "draft": null }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertSingleProperty(result, "draft", "must not be null");
		}

		@Test
		@DisplayName("PATCH com uma língua ausente no id retorna 400 type")
		void patchAbsentTypeReturns400() throws Exception {
			final MvcResult result = mockMvc.perform(patch(Routes.TERM + "/" + DRAFT_EN_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "PT", "content": "x" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertSingleProperty(result, "type", "recorded for this term");
		}

		@Test
		@DisplayName("PATCH com id inexistente retorna 404 sem corpo")
		void patchMissingReturns404() throws Exception {
			final MvcResult result = mockMvc.perform(patch(Routes.TERM + "/" + UNKNOWN_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "type": "EN", "content": "x" }
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}

		@Test
		@DisplayName("DELETE remove todas as linhas do id e retorna 204")
		void deleteReturns204() throws Exception {
			mockMvc.perform(delete(Routes.TERM + "/" + DRAFT_ALL_ID))
					.andExpect(status().isNoContent());
			assertThat(termRepository.findLinesById(DRAFT_ALL_ID)).isEmpty();
		}

		@Test
		@DisplayName("DELETE com id inexistente retorna 404 sem corpo")
		void deleteMissingReturns404() throws Exception {
			final MvcResult result = mockMvc.perform(delete(Routes.TERM + "/" + UNKNOWN_ID))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}
	}

}
