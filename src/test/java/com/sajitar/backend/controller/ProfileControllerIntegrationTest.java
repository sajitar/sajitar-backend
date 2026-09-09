package com.sajitar.backend.controller;

import static com.sajitar.backend.settlement.profile.ProfileSettlementFixture.ALICE_BIRTHDAY;
import static com.sajitar.backend.settlement.profile.ProfileSettlementFixture.ALICE_DESCRIPTION;
import static com.sajitar.backend.settlement.profile.ProfileSettlementFixture.ALICE_EMAIL;
import static com.sajitar.backend.settlement.profile.ProfileSettlementFixture.ALICE_ID;
import static com.sajitar.backend.settlement.profile.ProfileSettlementFixture.ALICE_NAME;
import static com.sajitar.backend.settlement.profile.ProfileSettlementFixture.NAME_SEARCH_NO_MATCH;
import static com.sajitar.backend.settlement.profile.ProfileSettlementFixture.NAME_SEARCH_QUEIROZ;
import static com.sajitar.backend.settlement.profile.ProfileSettlementFixture.NAME_SEARCH_SILVA;
import static com.sajitar.backend.settlement.profile.ProfileSettlementFixture.SETTLEMENT_ROW_COUNT;
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
import java.util.List;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sajitar.backend.adapter.in.web.profile.ProfileController;
import com.sajitar.backend.adapter.in.web.Routes;
import com.sajitar.backend.adapter.out.persistence.profile.ProfileJpaEntity;
import com.sajitar.backend.adapter.out.persistence.profile.ProfileJpaRepository;

/**
 * Integração do {@link ProfileController} com a massa
 * {@code classpath:settlement/profile.sql} (mesma cadeia que ambiente local:
 * funções, colunas, unicidades, índices e inserts — ver {@code src/test/resources/application.yml}).
 */
@SpringBootTest
@DisplayName("ProfileController (integração HTTP + settlement)")
class ProfileControllerIntegrationTest {

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private ProfileJpaRepository profileRepository;

	/** Mesma leitura JSON da API; não depende de bean {@code ObjectMapper} no contexto. */
	private final ObjectMapper objectMapper = new ObjectMapper();

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

	/** Primeira página (sem cursor): só {@code followingElements}; {@code precedingElements} permanece 0. */
	private static long findAllFollowingAfterLast(final ProfileJpaRepository repo, final List<ProfileJpaEntity> page,
			final boolean reverse) {
		final var last = page.getLast();
		return reverse ? repo.countForFindAllDescendingAfter(last.getName(), last.getId())
				: repo.countForFindAllAscendingAfter(last.getName(), last.getId());
	}

	/** Página de continuação: itens antes do primeiro (ordenação oposta), alinhado a {@code precedingElements}. */
	private static long findAllPrecedingAfterFirst(final ProfileJpaRepository repo, final List<ProfileJpaEntity> page,
			final boolean reverse) {
		final var first = page.getFirst();
		return reverse ? repo.countForFindAllAscendingAfter(first.getName(), first.getId())
				: repo.countForFindAllDescendingAfter(first.getName(), first.getId());
	}

	private static long nameSearchFollowingAfterLast(final ProfileJpaRepository repo, final List<ProfileJpaEntity> page,
			final boolean reverse, final String name) {
		final var last = page.getLast();
		return reverse ? repo.countForFindByNameContainingIgnoreCaseDescendingAfter(last.getName(), last.getId(), name)
				: repo.countForFindByNameContainingIgnoreCaseAscendingAfter(last.getName(), last.getId(), name);
	}

	private static long nameSearchPrecedingAfterFirst(final ProfileJpaRepository repo, final List<ProfileJpaEntity> page,
			final boolean reverse, final String name) {
		final var first = page.getFirst();
		return reverse ? repo.countForFindByNameContainingIgnoreCaseAscendingAfter(first.getName(), first.getId(), name)
				: repo.countForFindByNameContainingIgnoreCaseDescendingAfter(first.getName(), first.getId(), name);
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

	private void assertBadRequestSingleProperty(final MvcResult result, final String propertyKey,
			final String... messageSubstrings) throws Exception {
		assertThat(result.getResponse().getContentType()).as("Content-Type do 400").contains("json");
		final JsonNode root = objectMapper.readTree(responseBodyUtf8(result));
		assertThat(root.isObject()).isTrue();
		assertThat(jsonObjectKeys(root)).containsExactly(propertyKey);
		final JsonNode arr = root.get(propertyKey);
		assertThat(arr.isArray()).isTrue();
		assertThat(arr.size()).as("número de mensagens em %s", propertyKey).isEqualTo(1);
		assertThat(arr.get(0).isTextual()).as("mensagem em %s deve ser string", propertyKey).isTrue();
		final String text = arr.get(0).asText();
		for (final String part : messageSubstrings) {
			assertThat(text).as("mensagem de validação em %s", propertyKey).contains(part);
		}
	}

	private void assertProfileSummaryNode(final JsonNode node, final ProfileJpaEntity expected) {
		assertThat(jsonObjectKeys(node)).containsExactlyInAnyOrder("id", "name", "description");
		assertThat(node.get("id").asText()).isEqualTo(expected.getId().toString());
		assertThat(node.get("name").asText()).isEqualTo(expected.getName());
		final JsonNode desc = node.get("description");
		if (expected.getDescription() == null) {
			assertThat(desc.isNull()).isTrue();
		} else {
			assertThat(desc.asText()).isEqualTo(expected.getDescription());
		}
	}

	private void assertPaginationJson(final String json, final List<ProfileJpaEntity> expectedContent, final boolean reverse,
			final long preceding, final long following) throws Exception {
		final JsonNode root = objectMapper.readTree(json);
		assertThat(root.isObject()).isTrue();
		assertThat(jsonObjectKeys(root)).containsExactlyInAnyOrder("content", "precedingElements", "followingElements",
				"reverse");
		assertThat(root.get("reverse").booleanValue()).isEqualTo(reverse);
		assertThat(root.get("precedingElements").asLong()).isEqualTo(preceding);
		assertThat(root.get("followingElements").asLong()).isEqualTo(following);
		final JsonNode content = root.get("content");
		assertThat(content.isArray()).isTrue();
		assertThat(content.size()).isEqualTo(expectedContent.size());
		for (int i = 0; i < expectedContent.size(); i++) {
			assertProfileSummaryNode(content.get(i), expectedContent.get(i));
		}
	}

	private void assertPaginationMvcResult(final MvcResult result, final List<ProfileJpaEntity> expectedContent,
			final boolean reverse, final long preceding, final long following) throws Exception {
		assertThat(result.getResponse().getContentType()).contains("json");
		assertPaginationJson(responseBodyUtf8(result), expectedContent, reverse, preceding, following);
	}

	@Nested
	@DisplayName("GET /profiles/{id}")
	class GetById {

		@Test
		@DisplayName("200, JSON com id, name e description (dados reais: Alice, settlement)")
		void returns200WithAlice() throws Exception {
			final ProfileJpaEntity alice = profileRepository.findById(ALICE_ID).orElseThrow();
			final MvcResult result = mockMvc.perform(get(Routes.PROFILE + "/" + ALICE_ID).accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andReturn();
			assertThat(result.getResponse().getContentType()).contains("json");
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactlyInAnyOrder("id", "name", "description");
			assertProfileSummaryNode(n, alice);
		}

		@Test
		@DisplayName("404 sem corpo quando o perfil não existe")
		void returns404WhenMissing() throws Exception {
			final var result = mockMvc
					.perform(get(Routes.PROFILE + "/" + UNKNOWN_ID).accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}

		@Test
		@DisplayName("400 quando o id na URL não é um UUID válido")
		void returns400WhenPathIdMalformed() throws Exception {
			final MvcResult result = mockMvc.perform(get(Routes.PROFILE + "/não-é-uuid").accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "id", "UUID");
		}
	}

	@Nested
	@DisplayName("GET /profiles/{id}/details")
	class GetDetails {

		@Test
		@DisplayName("200 com DTO de detalhe: email e birthday alinhados à massa de settlement")
		void returns200WithAllDetailFields() throws Exception {
			final MvcResult result = mockMvc
					.perform(get(Routes.PROFILE + "/" + ALICE_ID + "/details").accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andReturn();
			assertThat(result.getResponse().getContentType()).contains("json");
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactlyInAnyOrder("id", "name", "description", "birthday", "email");
			assertThat(n.get("id").asText()).isEqualTo(ALICE_ID.toString());
			assertThat(n.get("name").asText()).isEqualTo(ALICE_NAME);
			assertThat(n.get("description").asText()).isEqualTo(ALICE_DESCRIPTION);
			assertThat(n.get("birthday").asText()).isEqualTo(ALICE_BIRTHDAY);
			assertThat(n.get("email").asText()).isEqualTo(ALICE_EMAIL);
		}

		@Test
		@DisplayName("404 sem corpo quando o perfil não existe")
		void returns404WhenMissing() throws Exception {
			final var result = mockMvc
					.perform(get(Routes.PROFILE + "/" + UNKNOWN_ID + "/details")
							.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}

		@Test
		@DisplayName("400 quando o id na URL não é um UUID")
		void returns400WhenPathIdMalformed() throws Exception {
			final MvcResult result = mockMvc
					.perform(get(Routes.PROFILE + "/abc/details").accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "id", "UUID");
		}
	}

	@Nested
	@DisplayName("GET /profiles (settlement: listagem, paginação, busca)")
	class GetProfiles {

		@Test
		@DisplayName("Primeira página: default limit=100, reverse=false; massa settlement + contadores de paginação")
		void firstPageDefaultMatchesRepository() throws Exception {
			assertThat(profileRepository.countForFindAll())
					.as("Número de linhas no script settlement/profile.sql")
					.isEqualTo(SETTLEMENT_ROW_COUNT);
			final var expected = profileRepository.findAllAscending(100);
			assertThat(expected).hasSize(100);
			final long following = findAllFollowingAfterLast(profileRepository, expected, false);
			final MvcResult result = mockMvc.perform(get(Routes.PROFILE).accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andReturn();
			assertPaginationMvcResult(result, expected, false, 0, following);
		}

		@Test
		@DisplayName("Primeira página: default limit=100, reverse=true; contadores alinhados ao repositório")
		void firstPageDefaultWithReverseTrueMatchesRepository() throws Exception {
			final var expected = profileRepository.findAllDescending(100);
			assertThat(expected).hasSize(100);
			final long following = findAllFollowingAfterLast(profileRepository, expected, true);
			final MvcResult result = mockMvc.perform(get(Routes.PROFILE).param("reverse", "true").accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andReturn();
			assertPaginationMvcResult(result, expected, true, 0, following);
		}

		@Test
		@DisplayName("limit=5: primeiros 5 itens idênticos ao repositório e followingElements")
		void firstFiveAlignWithRepository() throws Exception {
			final var five = profileRepository.findAllAscending(5);
			assertThat(five).hasSize(5);
			final long following = findAllFollowingAfterLast(profileRepository, five, false);
			final MvcResult result = mockMvc.perform(get(Routes.PROFILE).param("limit", "5").accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andReturn();
			assertPaginationMvcResult(result, five, false, 0, following);
		}

		@Test
		@DisplayName("limit=5 e reverse=true: idênticos a findAllDescending(5) e contadores")
		void firstFiveWithReverseTrueAlignWithRepository() throws Exception {
			final var five = profileRepository.findAllDescending(5);
			assertThat(five).hasSize(5);
			final long following = findAllFollowingAfterLast(profileRepository, five, true);
			final MvcResult result = mockMvc.perform(get(Routes.PROFILE).param("limit", "5").param("reverse", "true")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andReturn();
			assertPaginationMvcResult(result, five, true, 0, following);
		}

		@Test
		@DisplayName("reverse=true e limit=3: alinha a findAllDescending(3) no banco e contadores")
		void reverseDescendingAligns() throws Exception {
			final var desc = profileRepository.findAllDescending(3);
			assertThat(desc).hasSize(3);
			final long following = findAllFollowingAfterLast(profileRepository, desc, true);
			final MvcResult result = mockMvc.perform(get(Routes.PROFILE).param("limit", "3").param("reverse", "true")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andReturn();
			assertPaginationMvcResult(result, desc, true, 0, following);
		}

		@Test
		@DisplayName("Busca name=Silva: página inteira, IDs e followingElements alinhados ao repositório")
		void nameSearchAligns() throws Exception {
			final var firstPage = profileRepository.findByNameContainingIgnoreCaseAscending(50, NAME_SEARCH_SILVA);
			assertThat(firstPage).isNotEmpty();
			final long following = nameSearchFollowingAfterLast(profileRepository, firstPage, false, NAME_SEARCH_SILVA);
			final MvcResult result = mockMvc.perform(get(Routes.PROFILE)
					.param("name", NAME_SEARCH_SILVA)
					.param("limit", "50")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andReturn();
			assertPaginationMvcResult(result, firstPage, false, 0, following);
		}

		@Test
		@DisplayName("Busca name=Silva e reverse=true: página inteira e contadores alinhados ao repositório")
		void nameSearchWithReverseTrueAligns() throws Exception {
			final var firstPage = profileRepository.findByNameContainingIgnoreCaseDescending(50, NAME_SEARCH_SILVA);
			assertThat(firstPage).isNotEmpty();
			final long following = nameSearchFollowingAfterLast(profileRepository, firstPage, true, NAME_SEARCH_SILVA);
			final MvcResult result = mockMvc.perform(get(Routes.PROFILE)
					.param("name", NAME_SEARCH_SILVA)
					.param("limit", "50")
					.param("reverse", "true")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andReturn();
			assertPaginationMvcResult(result, firstPage, true, 0, following);
		}

		@Test
		@DisplayName("Busca por nome: segunda página (cursor) alinha a findByName...AscendingAfter no repositório")
		void nameSearchSecondPageByCursor() throws Exception {
			final var allMatches = profileRepository.findByNameContainingIgnoreCaseAscending(500, NAME_SEARCH_QUEIROZ);
			assertThat(allMatches)
					.as("settlement: ao menos 3 ocorrências de '%s' (ex.: família Queiroz no script)", NAME_SEARCH_QUEIROZ)
					.hasSizeGreaterThanOrEqualTo(3);
			final int pageSize = 2;
			final var page1 = profileRepository.findByNameContainingIgnoreCaseAscending(pageSize, NAME_SEARCH_QUEIROZ);
			assertThat(page1).hasSize(pageSize);
			final var last1 = page1.getLast();
			final var page2FromRepo = profileRepository.findByNameContainingIgnoreCaseAscendingAfter(pageSize, last1.getName(),
					last1.getId(), NAME_SEARCH_QUEIROZ);
			assertThat(page2FromRepo).isNotEmpty();
			final long followingP1 = nameSearchFollowingAfterLast(profileRepository, page1, false, NAME_SEARCH_QUEIROZ);
			final MvcResult page1Mvc = mockMvc.perform(get(Routes.PROFILE)
					.param("name", NAME_SEARCH_QUEIROZ)
					.param("limit", String.valueOf(pageSize))
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andReturn();
			assertPaginationMvcResult(page1Mvc, page1, false, 0, followingP1);
			final long followingP2 = nameSearchFollowingAfterLast(profileRepository, page2FromRepo, false,
					NAME_SEARCH_QUEIROZ);
			final long precedingP2 = nameSearchPrecedingAfterFirst(profileRepository, page2FromRepo, false,
					NAME_SEARCH_QUEIROZ);
			final MvcResult page2Mvc = mockMvc.perform(get(Routes.PROFILE)
					.param("name", NAME_SEARCH_QUEIROZ)
					.param("limit", String.valueOf(pageSize))
					.param("lastSeenName", last1.getName())
					.param("lastSeenId", last1.getId().toString())
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andReturn();
			assertPaginationMvcResult(page2Mvc, page2FromRepo, false, precedingP2, followingP2);
		}

		@Test
		@DisplayName("Busca por nome (reverse): segunda página (cursor) alinha a findByName...DescendingAfter no repositório")
		void nameSearchSecondPageByCursorWithReverseTrue() throws Exception {
			final var allMatches = profileRepository.findByNameContainingIgnoreCaseDescending(500, NAME_SEARCH_QUEIROZ);
			assertThat(allMatches)
					.as("settlement: ao menos 3 ocorrências de '%s' (ex.: família Queiroz no script)", NAME_SEARCH_QUEIROZ)
					.hasSizeGreaterThanOrEqualTo(3);
			final int pageSize = 2;
			final var page1 = profileRepository.findByNameContainingIgnoreCaseDescending(pageSize, NAME_SEARCH_QUEIROZ);
			assertThat(page1).hasSize(pageSize);
			final var last1 = page1.getLast();
			final var page2FromRepo = profileRepository.findByNameContainingIgnoreCaseDescendingAfter(pageSize, last1.getName(),
					last1.getId(), NAME_SEARCH_QUEIROZ);
			assertThat(page2FromRepo).isNotEmpty();
			final long followingP1 = nameSearchFollowingAfterLast(profileRepository, page1, true, NAME_SEARCH_QUEIROZ);
			final MvcResult page1Mvc = mockMvc.perform(get(Routes.PROFILE)
					.param("name", NAME_SEARCH_QUEIROZ)
					.param("limit", String.valueOf(pageSize))
					.param("reverse", "true")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andReturn();
			assertPaginationMvcResult(page1Mvc, page1, true, 0, followingP1);
			final long followingP2 = nameSearchFollowingAfterLast(profileRepository, page2FromRepo, true,
					NAME_SEARCH_QUEIROZ);
			final long precedingP2 = nameSearchPrecedingAfterFirst(profileRepository, page2FromRepo, true,
					NAME_SEARCH_QUEIROZ);
			final MvcResult page2Mvc = mockMvc.perform(get(Routes.PROFILE)
					.param("name", NAME_SEARCH_QUEIROZ)
					.param("limit", String.valueOf(pageSize))
					.param("reverse", "true")
					.param("lastSeenName", last1.getName())
					.param("lastSeenId", last1.getId().toString())
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andReturn();
			assertPaginationMvcResult(page2Mvc, page2FromRepo, true, precedingP2, followingP2);
		}

		@Test
		@DisplayName("Busca por nome: três blocos (limit=1) — avança 1o, 2o e 3o resultado na ordenação asc")
		void nameSearchAdvancesWithLimitOne() throws Exception {
			final var allMatches = profileRepository.findByNameContainingIgnoreCaseAscending(500, NAME_SEARCH_QUEIROZ);
			assertThat(allMatches)
					.as("mínimo 3 ocorrências de '%s' para três requisições com limit=1", NAME_SEARCH_QUEIROZ)
					.hasSizeGreaterThanOrEqualTo(3);
			final int limit = 1;
			for (int i = 0; i < 3; i++) {
				MockHttpServletRequestBuilder request = get(Routes.PROFILE)
						.param("name", NAME_SEARCH_QUEIROZ)
						.param("limit", String.valueOf(limit))
						.accept(MediaType.APPLICATION_JSON);
				if (i > 0) {
					final var prev = allMatches.get(i - 1);
					request = request.param("lastSeenName", prev.getName())
							.param("lastSeenId", prev.getId().toString());
				}
				final var page = List.of(allMatches.get(i));
				final long following = nameSearchFollowingAfterLast(profileRepository, page, false, NAME_SEARCH_QUEIROZ);
				final long preceding = i == 0 ? 0L
						: nameSearchPrecedingAfterFirst(profileRepository, page, false, NAME_SEARCH_QUEIROZ);
				final MvcResult r = mockMvc.perform(request)
						.andExpect(status().isOk())
						.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
						.andReturn();
				assertPaginationMvcResult(r, page, false, preceding, following);
			}
		}

		@Test
		@DisplayName("Busca por nome: três blocos (limit=1, reverse=true) — 1o, 2o e 3o na ordenação desc")
		void nameSearchAdvancesWithLimitOneWithReverseTrue() throws Exception {
			final var allMatches = profileRepository.findByNameContainingIgnoreCaseDescending(500, NAME_SEARCH_QUEIROZ);
			assertThat(allMatches)
					.as("mínimo 3 ocorrências de '%s' para três requisições com limit=1 e reverse=true", NAME_SEARCH_QUEIROZ)
					.hasSizeGreaterThanOrEqualTo(3);
			final int limit = 1;
			for (int i = 0; i < 3; i++) {
				MockHttpServletRequestBuilder request = get(Routes.PROFILE)
						.param("name", NAME_SEARCH_QUEIROZ)
						.param("limit", String.valueOf(limit))
						.param("reverse", "true")
						.accept(MediaType.APPLICATION_JSON);
				if (i > 0) {
					final var prev = allMatches.get(i - 1);
					request = request.param("lastSeenName", prev.getName())
							.param("lastSeenId", prev.getId().toString());
				}
				final var page = List.of(allMatches.get(i));
				final long following = nameSearchFollowingAfterLast(profileRepository, page, true, NAME_SEARCH_QUEIROZ);
				final long preceding = i == 0 ? 0L
						: nameSearchPrecedingAfterFirst(profileRepository, page, true, NAME_SEARCH_QUEIROZ);
				final MvcResult r = mockMvc.perform(request)
						.andExpect(status().isOk())
						.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
						.andReturn();
				assertPaginationMvcResult(r, page, true, preceding, following);
			}
		}

		@Test
		@DisplayName("Continuação de cursor: segunda página (limit=1) após o primeiro da ordering asc")
		void secondPageByCursor() throws Exception {
			final var first = profileRepository.findAllAscending(1);
			assertThat(first).hasSize(1);
			final var secondPage = profileRepository.findAllAscendingAfter(1, first.getFirst().getName(),
					first.getFirst().getId());
			assertThat(secondPage).hasSize(1);
			final long following = findAllFollowingAfterLast(profileRepository, secondPage, false);
			final long preceding = findAllPrecedingAfterFirst(profileRepository, secondPage, false);
			final MvcResult r = mockMvc.perform(get(Routes.PROFILE)
					.param("limit", "1")
					.param("lastSeenName", first.getFirst().getName())
					.param("lastSeenId", first.getFirst().getId().toString())
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andReturn();
			assertPaginationMvcResult(r, secondPage, false, preceding, following);
		}

		@Test
		@DisplayName("Continuação de cursor (reverse): segunda página (limit=1) após o primeiro da ordering desc")
		void secondPageByCursorWithReverseTrue() throws Exception {
			final var first = profileRepository.findAllDescending(1);
			assertThat(first).hasSize(1);
			final var secondPage = profileRepository.findAllDescendingAfter(1, first.getFirst().getName(),
					first.getFirst().getId());
			assertThat(secondPage).hasSize(1);
			final long following = findAllFollowingAfterLast(profileRepository, secondPage, true);
			final long preceding = findAllPrecedingAfterFirst(profileRepository, secondPage, true);
			final MvcResult r = mockMvc.perform(get(Routes.PROFILE)
					.param("limit", "1")
					.param("reverse", "true")
					.param("lastSeenName", first.getFirst().getName())
					.param("lastSeenId", first.getFirst().getId().toString())
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andReturn();
			assertPaginationMvcResult(r, secondPage, true, preceding, following);
		}

		@ParameterizedTest(name = "limit={0} reverse={1}")
		@CsvSource({ "0,false", "0,true", "101,false", "101,true" })
		@DisplayName("400 quando limit está fora do intervalo @Limit (0 ou acima do máximo)")
		void badRequestWhenLimitOutOfRange(final int limit, final boolean reverse) throws Exception {
			var req = get(Routes.PROFILE).param("limit", String.valueOf(limit));
			if (reverse) {
				req = req.param("reverse", "true");
			}
			final MvcResult br = mockMvc.perform(req.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(br, "limit", "positive");
		}

		@Test
		@DisplayName("400 quando limit não é numérico (MethodArgumentTypeMismatchException)")
		void badRequestWhenLimitNotNumeric() throws Exception {
			final MvcResult br = mockMvc.perform(get(Routes.PROFILE).param("limit", "cinco").accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(br, "limit", "belong", "type");
		}

		@Test
		@DisplayName("400 quando reverse não é booleano válido")
		void badRequestWhenReverseNotBoolean() throws Exception {
			final MvcResult br = mockMvc
					.perform(get(Routes.PROFILE).param("reverse", "talvez").accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(br, "reverse", "belong", "type");
		}

		@Test
		@DisplayName("name só com espaços: hasText=false; controlador aplica listagem geral (como local)")
		void whitespaceOnlyNameFallsBackToListAll() throws Exception {
			final var expected = profileRepository.findAllAscending(20);
			assertThat(expected).isNotEmpty();
			final long following = findAllFollowingAfterLast(profileRepository, expected, false);
			final MvcResult result = mockMvc.perform(get(Routes.PROFILE).param("name", "   ").param("limit", "20")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andReturn();
			assertPaginationMvcResult(result, expected, false, 0, following);
		}

		@Test
		@DisplayName("name só com espaços e reverse=true: listagem geral desc (hasText ainda falso)")
		void whitespaceOnlyNameFallsBackToListAllWithReverseTrue() throws Exception {
			final var expected = profileRepository.findAllDescending(20);
			assertThat(expected).isNotEmpty();
			final long following = findAllFollowingAfterLast(profileRepository, expected, true);
			final MvcResult result = mockMvc.perform(get(Routes.PROFILE).param("name", "   ").param("limit", "20")
					.param("reverse", "true")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andReturn();
			assertPaginationMvcResult(result, expected, true, 0, following);
		}

		@ParameterizedTest(name = "reverse={0}")
		@ValueSource(booleans = { false, true })
		@DisplayName("400 lastSeenName inválido (@Name) com busca por nome e cursor completo")
		void badRequestWhenLastSeenNameInvalidWithCursor(final boolean reverse) throws Exception {
			var req = get(Routes.PROFILE)
					.param("name", NAME_SEARCH_SILVA)
					.param("lastSeenName", "Maria@Silva")
					.param("lastSeenId", ALICE_ID.toString());
			if (reverse) {
				req = req.param("reverse", "true");
			}
			final MvcResult br = mockMvc.perform(req.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(br, "lastSeenName", "well-formed name");
		}

		@Test
		@DisplayName("400 lastSeenId inválido com busca e cursor completo (tipo UUID)")
		void badRequestWhenLastSeenIdMalformedWithNameCursor() throws Exception {
			final var cursorName = profileRepository.findByNameContainingIgnoreCaseAscending(1, NAME_SEARCH_SILVA)
					.getFirst()
					.getName();
			final MvcResult br = mockMvc.perform(get(Routes.PROFILE)
					.param("name", NAME_SEARCH_SILVA)
					.param("lastSeenName", cursorName)
					.param("lastSeenId", "não-uuid")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(br, "lastSeenId", "UUID");
		}

		@Test
		@DisplayName("400 lastSeenId inválido na listagem geral com cursor completo")
		void badRequestWhenLastSeenIdMalformedFindAllCursor() throws Exception {
			final var anchor = profileRepository.findAllAscending(1).getFirst();
			final MvcResult br = mockMvc.perform(get(Routes.PROFILE)
					.param("lastSeenName", anchor.getName())
					.param("lastSeenId", "xyz")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(br, "lastSeenId", "UUID");
		}

		@Test
		@DisplayName("404 sem corpo: cursor além do último item na listagem geral (asc)")
		void returns404WhenCursorAfterLastPageFindAll() throws Exception {
			final var last = profileRepository.findAllAscending(1_000).getLast();
			final var result = mockMvc
					.perform(get(Routes.PROFILE)
							.param("lastSeenName", last.getName())
							.param("lastSeenId", last.getId().toString())
							.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}

		@Test
		@DisplayName("404 sem corpo: cursor além do último item na listagem geral (desc, reverse=true)")
		void returns404WhenCursorAfterLastPageFindAllWithReverseTrue() throws Exception {
			final var last = profileRepository.findAllDescending(1_000).getLast();
			final var result = mockMvc
					.perform(get(Routes.PROFILE)
							.param("reverse", "true")
							.param("lastSeenName", last.getName())
							.param("lastSeenId", last.getId().toString())
							.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}

		@Test
		@DisplayName("404 sem corpo: cursor além do último item na busca por nome (asc)")
		void returns404WhenCursorAfterLastPageNameSearch() throws Exception {
			final var matches = profileRepository.findByNameContainingIgnoreCaseAscending(500, NAME_SEARCH_QUEIROZ);
			assertThat(matches).isNotEmpty();
			final var last = matches.getLast();
			final var result = mockMvc
					.perform(get(Routes.PROFILE)
							.param("name", NAME_SEARCH_QUEIROZ)
							.param("lastSeenName", last.getName())
							.param("lastSeenId", last.getId().toString())
							.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}

		@Test
		@DisplayName("404 sem corpo: cursor além do último item na busca por nome (desc, reverse=true)")
		void returns404WhenCursorAfterLastPageNameSearchWithReverseTrue() throws Exception {
			final var matches = profileRepository.findByNameContainingIgnoreCaseDescending(500, NAME_SEARCH_QUEIROZ);
			assertThat(matches).isNotEmpty();
			final var last = matches.getLast();
			final var result = mockMvc
					.perform(get(Routes.PROFILE)
							.param("name", NAME_SEARCH_QUEIROZ)
							.param("reverse", "true")
							.param("lastSeenName", last.getName())
							.param("lastSeenId", last.getId().toString())
							.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}

		@ParameterizedTest(name = "reverse={0}")
		@ValueSource(booleans = { false, true })
		@DisplayName("404 sem corpo: busca por nome sem nenhum resultado (primeira página vazia)")
		void returns404WhenNameSearchHasNoMatches(final boolean reverse) throws Exception {
			if (reverse) {
				assertThat(profileRepository.findByNameContainingIgnoreCaseDescending(50, NAME_SEARCH_NO_MATCH)).isEmpty();
			} else {
				assertThat(profileRepository.findByNameContainingIgnoreCaseAscending(50, NAME_SEARCH_NO_MATCH)).isEmpty();
			}
			var req = get(Routes.PROFILE).param("name", NAME_SEARCH_NO_MATCH).param("limit", "50");
			if (reverse) {
				req = req.param("reverse", "true");
			}
			final var result = mockMvc.perform(req.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}

		@Test
		@DisplayName("name vazio: hasText=false; mesma primeira página que listagem geral (asc)")
		void emptyNameParamFallsBackToListAll() throws Exception {
			final var expected = profileRepository.findAllAscending(15);
			assertThat(expected).isNotEmpty();
			final long following = findAllFollowingAfterLast(profileRepository, expected, false);
			final MvcResult result = mockMvc.perform(get(Routes.PROFILE).param("name", "").param("limit", "15")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andReturn();
			assertPaginationMvcResult(result, expected, false, 0, following);
		}

		@Test
		@DisplayName("name vazio e reverse=true: listagem geral desc")
		void emptyNameParamFallsBackToListAllWithReverseTrue() throws Exception {
			final var expected = profileRepository.findAllDescending(15);
			assertThat(expected).isNotEmpty();
			final long following = findAllFollowingAfterLast(profileRepository, expected, true);
			final MvcResult result = mockMvc.perform(get(Routes.PROFILE).param("name", "").param("limit", "15")
					.param("reverse", "true")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andReturn();
			assertPaginationMvcResult(result, expected, true, 0, following);
		}

		@Test
		@DisplayName("Só lastSeenId (sem lastSeenName): condição de cursor falsa; primeira página findAll asc")
		void partialCursorOnlyLastSeenIdIgnoredUsesFirstPageFindAll() throws Exception {
			final var expected = profileRepository.findAllAscending(7);
			assertThat(expected).hasSize(7);
			final long following = findAllFollowingAfterLast(profileRepository, expected, false);
			final MvcResult result = mockMvc.perform(get(Routes.PROFILE)
					.param("limit", "7")
					.param("lastSeenId", expected.getLast().getId().toString())
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andReturn();
			assertPaginationMvcResult(result, expected, false, 0, following);
		}

		@Test
		@DisplayName("Só lastSeenId e reverse=true: primeira página findAll desc (cursor ignorado)")
		void partialCursorOnlyLastSeenIdIgnoredUsesFirstPageFindAllWithReverseTrue() throws Exception {
			final var expected = profileRepository.findAllDescending(7);
			assertThat(expected).hasSize(7);
			final long following = findAllFollowingAfterLast(profileRepository, expected, true);
			final MvcResult result = mockMvc.perform(get(Routes.PROFILE)
					.param("limit", "7")
					.param("reverse", "true")
					.param("lastSeenId", expected.getLast().getId().toString())
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andReturn();
			assertPaginationMvcResult(result, expected, true, 0, following);
		}

		@Test
		@DisplayName("Só lastSeenName (sem lastSeenId): cursor incompleto; primeira página findAll asc")
		void partialCursorOnlyLastSeenNameIgnoredUsesFirstPageFindAll() throws Exception {
			final var expected = profileRepository.findAllAscending(6);
			assertThat(expected).hasSize(6);
			final var markerName = expected.getLast().getName();
			final long following = findAllFollowingAfterLast(profileRepository, expected, false);
			final MvcResult result = mockMvc.perform(get(Routes.PROFILE)
					.param("limit", "6")
					.param("lastSeenName", markerName)
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andReturn();
			assertPaginationMvcResult(result, expected, false, 0, following);
		}

		@Test
		@DisplayName("Só lastSeenName e reverse=true: primeira página findAll desc")
		void partialCursorOnlyLastSeenNameIgnoredUsesFirstPageFindAllWithReverseTrue() throws Exception {
			final var expected = profileRepository.findAllDescending(6);
			assertThat(expected).hasSize(6);
			final long following = findAllFollowingAfterLast(profileRepository, expected, true);
			final MvcResult result = mockMvc.perform(get(Routes.PROFILE)
					.param("limit", "6")
					.param("reverse", "true")
					.param("lastSeenName", expected.getFirst().getName())
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andReturn();
			assertPaginationMvcResult(result, expected, true, 0, following);
		}

		@Test
		@DisplayName("Busca: só lastSeenId sem lastSeenName — primeira página da busca (asc)")
		void partialCursorOnlyLastSeenIdIgnoredUsesFirstPageNameSearch() throws Exception {
			final var expected = profileRepository.findByNameContainingIgnoreCaseAscending(8, NAME_SEARCH_SILVA);
			assertThat(expected).isNotEmpty();
			final long following = nameSearchFollowingAfterLast(profileRepository, expected, false, NAME_SEARCH_SILVA);
			final MvcResult result = mockMvc.perform(get(Routes.PROFILE)
					.param("name", NAME_SEARCH_SILVA)
					.param("limit", "8")
					.param("lastSeenId", ALICE_ID.toString())
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andReturn();
			assertPaginationMvcResult(result, expected, false, 0, following);
		}

		@Test
		@DisplayName("Busca: só lastSeenId, reverse=true — primeira página desc da busca")
		void partialCursorOnlyLastSeenIdIgnoredUsesFirstPageNameSearchWithReverseTrue() throws Exception {
			final var expected = profileRepository.findByNameContainingIgnoreCaseDescending(8, NAME_SEARCH_SILVA);
			assertThat(expected).isNotEmpty();
			final long following = nameSearchFollowingAfterLast(profileRepository, expected, true, NAME_SEARCH_SILVA);
			final MvcResult result = mockMvc.perform(get(Routes.PROFILE)
					.param("name", NAME_SEARCH_SILVA)
					.param("limit", "8")
					.param("reverse", "true")
					.param("lastSeenId", ALICE_ID.toString())
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andReturn();
			assertPaginationMvcResult(result, expected, true, 0, following);
		}

		@Test
		@DisplayName("Busca: só lastSeenName sem lastSeenId — primeira página asc")
		void partialCursorOnlyLastSeenNameIgnoredUsesFirstPageNameSearch() throws Exception {
			final var expected = profileRepository.findByNameContainingIgnoreCaseAscending(8, NAME_SEARCH_QUEIROZ);
			assertThat(expected).hasSizeGreaterThanOrEqualTo(2);
			final var markerName = expected.get(1).getName();
			final long following = nameSearchFollowingAfterLast(profileRepository, expected, false, NAME_SEARCH_QUEIROZ);
			final MvcResult result = mockMvc.perform(get(Routes.PROFILE)
					.param("name", NAME_SEARCH_QUEIROZ)
					.param("limit", "8")
					.param("lastSeenName", markerName)
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andReturn();
			assertPaginationMvcResult(result, expected, false, 0, following);
		}

		@Test
		@DisplayName("Busca: só lastSeenName, reverse=true — primeira página desc")
		void partialCursorOnlyLastSeenNameIgnoredUsesFirstPageNameSearchWithReverseTrue() throws Exception {
			final var expected = profileRepository.findByNameContainingIgnoreCaseDescending(8, NAME_SEARCH_QUEIROZ);
			assertThat(expected).hasSizeGreaterThanOrEqualTo(2);
			final var markerName = expected.get(1).getName();
			final long following = nameSearchFollowingAfterLast(profileRepository, expected, true, NAME_SEARCH_QUEIROZ);
			final MvcResult result = mockMvc.perform(get(Routes.PROFILE)
					.param("name", NAME_SEARCH_QUEIROZ)
					.param("limit", "8")
					.param("reverse", "true")
					.param("lastSeenName", markerName)
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andReturn();
			assertPaginationMvcResult(result, expected, true, 0, following);
		}

		@Test
		@DisplayName("lastSeenName inválido sem lastSeenId: cursor incompleto; validação @Name não aplicada; 200")
		void invalidLastSeenNameAloneDoesNotTriggerValidationUsesFirstPage() throws Exception {
			final var expected = profileRepository.findAllAscending(4);
			assertThat(expected).hasSize(4);
			final long following = findAllFollowingAfterLast(profileRepository, expected, false);
			final MvcResult result = mockMvc.perform(get(Routes.PROFILE)
					.param("limit", "4")
					.param("lastSeenName", "Nome@Invalido")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andReturn();
			assertPaginationMvcResult(result, expected, false, 0, following);
		}
	}

	@Nested
	@Transactional
	@DisplayName("POST/PUT/PATCH/DELETE /profiles")
	class WriteProfiles {

		private static final String ALICE_PASSWORD_HASH = "$2a$10$7Z0zPEtZklljGNH8JHcnRO0pOZAVlBH36Fg7QO9N1LD4thimBL.TW";

		@Test
		@DisplayName("POST persiste senha em BCrypt e não devolve a senha no JSON")
		void postHashesPasswordAndOmitsItFromResponse() throws Exception {
			final MvcResult result = mockMvc.perform(post(Routes.PROFILE)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name": "Zaida Nova",
							  "description": "Perfil criado no teste de integração.",
							  "birthday": "1990-01-01",
							  "email": "zaida.nova@example.com",
							  "password": "senhaSegura1"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactlyInAnyOrder("id", "name", "description");
			assertThat(n.get("name").asText()).isEqualTo("Zaida Nova");
			final var persisted = profileRepository.findByEmail("zaida.nova@example.com").orElseThrow();
			assertThat(persisted.getPassword()).startsWith("$2a$");
			assertThat(persisted.getPassword()).isNotEqualTo("senhaSegura1");
			assertThat(persisted.getPassword()).hasSize(60);
		}

		@Test
		@DisplayName("POST com e-mail já registrado retorna 409")
		void postDuplicateEmailReturns409() throws Exception {
			final MvcResult result = mockMvc.perform(post(Routes.PROFILE)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name": "Alice Alves",
							  "description": "Uma pessoa criativa e dedicada.",
							  "birthday": "1988-01-10",
							  "email": "alice@example.com",
							  "password": "senhaSegura1"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isConflict())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("email");
			assertThat(n.get("email").get(0).asText()).contains("unregistered");
		}

		@Test
		@DisplayName("POST com corpo inválido retorna 400 (MethodArgumentNotValidException)")
		void postInvalidBodyReturns400() throws Exception {
			final MvcResult result = mockMvc.perform(post(Routes.PROFILE)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name": "123",
							  "description": "x",
							  "birthday": "1988-01-10",
							  "email": "alice@example.com",
							  "password": "senhaSegura1"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "name", "well-formed name");
		}

		@ParameterizedTest(name = "lang={0}")
		@CsvSource({
				"pt, bem formado",
				"es, bien formado"
		})
		@DisplayName("POST com nome inválido respeita query lang")
		void postInvalidNameRespectsLangQuery(final String lang, final String expectedPart) throws Exception {
			final MvcResult result = mockMvc.perform(post(Routes.PROFILE)
					.param("lang", lang)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name": "123",
							  "description": "x",
							  "birthday": "1988-01-10",
							  "email": "alice@example.com",
							  "password": "senhaSegura1"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "name", expectedPart);
		}

		@ParameterizedTest(name = "lang={0}")
		@CsvSource({
				"pt, não registrado",
				"es, no registrado"
		})
		@DisplayName("POST com e-mail já registrado respeita query lang")
		void postDuplicateEmailRespectsLangQuery(final String lang, final String expectedPart) throws Exception {
			final MvcResult result = mockMvc.perform(post(Routes.PROFILE)
					.param("lang", lang)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name": "Alice Alves",
							  "description": "Uma pessoa criativa e dedicada.",
							  "birthday": "1988-01-10",
							  "email": "alice@example.com",
							  "password": "senhaSegura1"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isConflict())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("email");
			assertThat(n.get("email").get(0).asText()).contains(expectedPart);
		}

		@Test
		@DisplayName("PUT sem senha mantém o hash atual")
		void putWithoutPasswordKeepsExistingHash() throws Exception {
			final MvcResult result = mockMvc.perform(put(Routes.PROFILE + "/" + ALICE_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name": "Alice Alves",
							  "description": "Descrição atualizada no teste.",
							  "birthday": "1988-01-10",
							  "email": "alice@example.com"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(n.get("description").asText()).isEqualTo("Descrição atualizada no teste.");
			final var persisted = profileRepository.findById(ALICE_ID).orElseThrow();
			assertThat(persisted.getPassword()).isEqualTo(ALICE_PASSWORD_HASH);
			assertThat(persisted.getDescription()).isEqualTo("Descrição atualizada no teste.");
		}

		@Test
		@DisplayName("PUT com senha recodifica o hash")
		void putWithPasswordRehashes() throws Exception {
			mockMvc.perform(put(Routes.PROFILE + "/" + ALICE_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name": "Alice Alves",
							  "description": "Uma pessoa criativa e dedicada.",
							  "birthday": "1988-01-10",
							  "email": "alice@example.com",
							  "password": "novaSenhaSegura1"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk());
			final var persisted = profileRepository.findById(ALICE_ID).orElseThrow();
			assertThat(persisted.getPassword()).startsWith("$2a$");
			assertThat(persisted.getPassword()).isNotEqualTo(ALICE_PASSWORD_HASH);
			assertThat(persisted.getPassword()).hasSize(60);
		}

		@Test
		@DisplayName("PUT com id inexistente retorna 404 sem corpo")
		void putUnknownIdReturns404() throws Exception {
			final var result = mockMvc.perform(put(Routes.PROFILE + "/" + UNKNOWN_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name": "Ninguem Existe",
							  "description": "x",
							  "birthday": "1988-01-10",
							  "email": "ninguem@example.com",
							  "password": "senhaSegura1"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}

		@Test
		@DisplayName("PUT ignora id no corpo e preserva o UUID da URL")
		void putIgnoresIdInBody() throws Exception {
			final MvcResult result = mockMvc.perform(put(Routes.PROFILE + "/" + ALICE_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "id": "%s",
							  "name": "Alice Alves",
							  "description": "Uma pessoa criativa e dedicada.",
							  "birthday": "1988-01-10",
							  "email": "alice@example.com"
							}
							""".formatted(UNKNOWN_ID))
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(n.get("id").asText()).isEqualTo(ALICE_ID.toString());
			final var persisted = profileRepository.findById(ALICE_ID).orElseThrow();
			assertThat(persisted.getId()).isEqualTo(ALICE_ID);
			assertThat(profileRepository.findById(UNKNOWN_ID)).isEmpty();
		}

		@Test
		@DisplayName("PATCH só o nome mantém descrição, e-mail e id")
		void patchOnlyName() throws Exception {
			final MvcResult result = mockMvc.perform(patch(Routes.PROFILE + "/" + ALICE_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name": "Alice Atualizada"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(n.get("id").asText()).isEqualTo(ALICE_ID.toString());
			assertThat(n.get("name").asText()).isEqualTo("Alice Atualizada");
			assertThat(n.get("description").asText()).isEqualTo(ALICE_DESCRIPTION);
			final var persisted = profileRepository.findById(ALICE_ID).orElseThrow();
			assertThat(persisted.getName()).isEqualTo("Alice Atualizada");
			assertThat(persisted.getDescription()).isEqualTo(ALICE_DESCRIPTION);
			assertThat(persisted.getEmail()).isEqualTo(ALICE_EMAIL);
			assertThat(persisted.getBirthday().toString()).isEqualTo(ALICE_BIRTHDAY);
		}

		@Test
		@DisplayName("PATCH descrição atualiza só a descrição")
		void patchDescription() throws Exception {
			final MvcResult result = mockMvc.perform(patch(Routes.PROFILE + "/" + ALICE_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "description": "Descrição atualizada no patch."
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(n.get("description").asText()).isEqualTo("Descrição atualizada no patch.");
			assertThat(n.get("name").asText()).isEqualTo(ALICE_NAME);
			final var persisted = profileRepository.findById(ALICE_ID).orElseThrow();
			assertThat(persisted.getDescription()).isEqualTo("Descrição atualizada no patch.");
			assertThat(persisted.getName()).isEqualTo(ALICE_NAME);
		}

		@Test
		@DisplayName("PATCH ignora id no corpo e preserva o UUID da URL")
		void patchIgnoresIdInBody() throws Exception {
			final MvcResult result = mockMvc.perform(patch(Routes.PROFILE + "/" + ALICE_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "id": "%s",
							  "name": "Alice Alves"
							}
							""".formatted(UNKNOWN_ID))
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(n.get("id").asText()).isEqualTo(ALICE_ID.toString());
			final var persisted = profileRepository.findById(ALICE_ID).orElseThrow();
			assertThat(persisted.getId()).isEqualTo(ALICE_ID);
			assertThat(profileRepository.findById(UNKNOWN_ID)).isEmpty();
		}

		@Test
		@DisplayName("PATCH com id inexistente retorna 404 sem corpo")
		void patchUnknownIdReturns404() throws Exception {
			final var result = mockMvc.perform(patch(Routes.PROFILE + "/" + UNKNOWN_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}

		@Test
		@DisplayName("PATCH com e-mail de outro perfil retorna 409")
		void patchDuplicateEmailReturns409() throws Exception {
			final MvcResult result = mockMvc.perform(patch(Routes.PROFILE + "/" + ALICE_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "email": "bruno@example.com"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isConflict())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("email");
			assertThat(n.get("email").get(0).asText()).contains("unregistered");
			final var alice = profileRepository.findById(ALICE_ID).orElseThrow();
			assertThat(alice.getEmail()).isEqualTo(ALICE_EMAIL);
		}

		@Test
		@DisplayName("PATCH com nome inválido retorna 400")
		void patchInvalidNameReturns400() throws Exception {
			final MvcResult result = mockMvc.perform(patch(Routes.PROFILE + "/" + ALICE_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name": "123"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "name", "well-formed name");
		}

		@ParameterizedTest(name = "lang={0}")
		@CsvSource({
				"pt, bem formado",
				"es, bien formado"
		})
		@DisplayName("PATCH com nome inválido respeita query lang")
		void patchInvalidNameRespectsLangQuery(final String lang, final String expectedPart) throws Exception {
			final MvcResult result = mockMvc.perform(patch(Routes.PROFILE + "/" + ALICE_ID)
					.param("lang", lang)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name": "123"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "name", expectedPart);
		}

		@ParameterizedTest(name = "lang={0}")
		@CsvSource({
				"pt, não registrado",
				"es, no registrado"
		})
		@DisplayName("PATCH com e-mail de outro perfil respeita query lang")
		void patchDuplicateEmailRespectsLangQuery(final String lang, final String expectedPart) throws Exception {
			final MvcResult result = mockMvc.perform(patch(Routes.PROFILE + "/" + ALICE_ID)
					.param("lang", lang)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "email": "bruno@example.com"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isConflict())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("email");
			assertThat(n.get("email").get(0).asText()).contains(expectedPart);
		}

		@Test
		@DisplayName("PATCH sem senha mantém o hash BCrypt atual")
		void patchWithoutPasswordKeepsExistingHash() throws Exception {
			final MvcResult result = mockMvc.perform(patch(Routes.PROFILE + "/" + ALICE_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name": "Alice Alves"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(n.get("name").asText()).isEqualTo(ALICE_NAME);
			final var persisted = profileRepository.findById(ALICE_ID).orElseThrow();
			assertThat(persisted.getPassword()).isEqualTo(ALICE_PASSWORD_HASH);
		}

		@Test
		@DisplayName("DELETE remove o perfil e retorna 204 sem corpo")
		void deleteExistingReturns204() throws Exception {
			assertThat(profileRepository.findById(ALICE_ID)).isPresent();
			final var result = mockMvc.perform(delete(Routes.PROFILE + "/" + ALICE_ID))
					.andExpect(status().isNoContent())
					.andReturn();
			assertNoContentBody(result);
			assertThat(profileRepository.findById(ALICE_ID)).isEmpty();
		}

		@Test
		@DisplayName("DELETE com id inexistente retorna 404 sem corpo")
		void deleteUnknownIdReturns404() throws Exception {
			final var result = mockMvc.perform(delete(Routes.PROFILE + "/" + UNKNOWN_ID))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}
	}
}
