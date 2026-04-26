package com.sajitar.backend.controller;

import static com.sajitar.backend.settlement.ProfileSettlementFixture.ALICE_BIRTHDAY;
import static com.sajitar.backend.settlement.ProfileSettlementFixture.ALICE_DESCRIPTION;
import static com.sajitar.backend.settlement.ProfileSettlementFixture.ALICE_EMAIL;
import static com.sajitar.backend.settlement.ProfileSettlementFixture.ALICE_ID;
import static com.sajitar.backend.settlement.ProfileSettlementFixture.ALICE_NAME;
import static com.sajitar.backend.settlement.ProfileSettlementFixture.NAME_SEARCH_QUEIROZ;
import static com.sajitar.backend.settlement.ProfileSettlementFixture.NAME_SEARCH_SILVA;
import static com.sajitar.backend.settlement.ProfileSettlementFixture.SETTLEMENT_ROW_COUNT;
import static com.sajitar.backend.settlement.ProfileSettlementFixture.UNKNOWN_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.sajitar.backend.repository.ProfileRepository;
import com.sajitar.backend.util.Routes;

/**
 * Integração do {@link ProfileController} com a massa
 * {@code classpath:settlement/profile.sql} (mesma cadeia que ambiente local:
 * funções, colunas, índices e inserts — ver {@code src/test/resources/application.yml}).
 */
@SpringBootTest
@DisplayName("ProfileController (integração HTTP + settlement)")
class ProfileControllerIntegrationTest {

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private ProfileRepository profileRepository;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

	@Nested
	@DisplayName("GET /profiles/{id}")
	class GetById {

		@Test
		@DisplayName("200, JSON com id, name e description (dados reais: Alice, settlement)")
		void returns200WithAlice() throws Exception {
			mockMvc.perform(get(Routes.PROFILE + "/" + ALICE_ID).accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andExpect(jsonPath("$.id").value(ALICE_ID.toString()))
					.andExpect(jsonPath("$.name").value(ALICE_NAME))
					.andExpect(jsonPath("$.description").value(ALICE_DESCRIPTION))
					.andExpect(jsonPath("$.email").doesNotExist())
					.andExpect(jsonPath("$.birthday").doesNotExist());
		}

		@Test
		@DisplayName("404 sem corpo quando o perfil não existe")
		void returns404WhenMissing() throws Exception {
			final var result = mockMvc
					.perform(get(Routes.PROFILE + "/" + UNKNOWN_ID).accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound())
					.andReturn();
			assertThat(result.getResponse().getContentAsString()).isEmpty();
		}

		@Test
		@DisplayName("400 quando o id na URL não é um UUID válido")
		void returns400WhenPathIdMalformed() throws Exception {
			mockMvc.perform(get(Routes.PROFILE + "/não-é-uuid").accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.id.length()").value(1))
					.andExpect(jsonPath("$.id[0]")
							.value(org.hamcrest.Matchers.containsString("deve pertencer ao tipo")));
		}
	}

	@Nested
	@DisplayName("GET /profiles/{id}/details")
	class GetDetails {

		@Test
		@DisplayName("200 com DTO de detalhe: email e birthday alinhados à massa de settlement")
		void returns200WithAllDetailFields() throws Exception {
			mockMvc.perform(get(Routes.PROFILE + "/" + ALICE_ID + "/details").accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andExpect(jsonPath("$.id").value(ALICE_ID.toString()))
					.andExpect(jsonPath("$.name").value(ALICE_NAME))
					.andExpect(jsonPath("$.description").value(ALICE_DESCRIPTION))
					.andExpect(jsonPath("$.birthday").value(ALICE_BIRTHDAY))
					.andExpect(jsonPath("$.email").value(ALICE_EMAIL));
		}

		@Test
		@DisplayName("404 sem corpo quando o perfil não existe")
		void returns404WhenMissing() throws Exception {
			final var result = mockMvc
					.perform(get(Routes.PROFILE + "/" + UNKNOWN_ID + "/details")
							.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound())
					.andReturn();
			assertThat(result.getResponse().getContentAsString()).isEmpty();
		}

		@Test
		@DisplayName("400 quando o id na URL não é um UUID")
		void returns400WhenPathIdMalformed() throws Exception {
			mockMvc.perform(get(Routes.PROFILE + "/abc/details").accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.id[0]")
							.value(org.hamcrest.Matchers.containsString("deve pertencer ao tipo")));
		}
	}

	@Nested
	@DisplayName("GET /profiles (settlement: listagem, paginação, busca)")
	class GetProfiles {

		@Test
		@DisplayName("A massa settlement foi carregada (135 perfis) — repositório e API coerentes")
		void totalCountMatchesScript() {
			assertThat(profileRepository.countForFindAll())
					.as("Número de linhas no script settlement/profile.sql")
					.isEqualTo(SETTLEMENT_ROW_COUNT);
		}

		@Test
		@DisplayName("Primeira página: default limit=100, reverse=false; alinha ao repositório (name_purified, id) asc")
		void firstPageDefaultMatchesRepository() throws Exception {
			final var expected = profileRepository.findAllAscending(100);
			assertThat(expected).hasSize(100);
			mockMvc.perform(get(Routes.PROFILE).accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.reverse").value(false))
					.andExpect(jsonPath("$.content.length()").value(100))
					.andExpect(jsonPath("$.content[0].id").value(expected.getFirst().getId().toString()))
					.andExpect(jsonPath("$.content[99].id").value(expected.getLast().getId().toString()));
		}

		@Test
		@DisplayName("Primeira página: default limit=100, reverse=true; alinha a findAllDescending(100) no banco")
		void firstPageDefaultWithReverseTrueMatchesRepository() throws Exception {
			final var expected = profileRepository.findAllDescending(100);
			assertThat(expected).hasSize(100);
			mockMvc.perform(get(Routes.PROFILE).param("reverse", "true").accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.reverse").value(true))
					.andExpect(jsonPath("$.content.length()").value(100))
					.andExpect(jsonPath("$.content[0].id").value(expected.getFirst().getId().toString()))
					.andExpect(jsonPath("$.content[99].id").value(expected.getLast().getId().toString()));
		}

		@Test
		@DisplayName("limit=5: primeiros 5 itens idênticos ao repositório")
		void firstFiveAlignWithRepository() throws Exception {
			final var five = profileRepository.findAllAscending(5);
			assertThat(five).hasSize(5);
			mockMvc.perform(get(Routes.PROFILE).param("limit", "5").accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content.length()").value(5))
					.andExpect(jsonPath("$.content[0].id").value(five.get(0).getId().toString()))
					.andExpect(jsonPath("$.content[4].id").value(five.get(4).getId().toString()));
		}

		@Test
		@DisplayName("limit=5 e reverse=true: primeiros 5 na ordenação desc idênticos a findAllDescending(5)")
		void firstFiveWithReverseTrueAlignWithRepository() throws Exception {
			final var five = profileRepository.findAllDescending(5);
			assertThat(five).hasSize(5);
			mockMvc.perform(get(Routes.PROFILE).param("limit", "5").param("reverse", "true")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.reverse").value(true))
					.andExpect(jsonPath("$.content.length()").value(5))
					.andExpect(jsonPath("$.content[0].id").value(five.get(0).getId().toString()))
					.andExpect(jsonPath("$.content[4].id").value(five.get(4).getId().toString()));
		}

		@Test
		@DisplayName("reverse=true e limit=3: alinha a findAllDescending(3) no banco")
		void reverseDescendingAligns() throws Exception {
			final var desc = profileRepository.findAllDescending(3);
			assertThat(desc).hasSize(3);
			mockMvc.perform(get(Routes.PROFILE).param("limit", "3").param("reverse", "true")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.reverse").value(true))
					.andExpect(jsonPath("$.content.length()").value(3))
					.andExpect(jsonPath("$.content[0].id").value(desc.get(0).getId().toString()));
		}

		@Test
		@DisplayName("Busca name=Silva: conteúdo alinhado à busca ascendente no repositório")
		void nameSearchAligns() throws Exception {
			final var firstPage = profileRepository.findByNameContainingIgnoreCaseAscending(50, NAME_SEARCH_SILVA);
			assertThat(firstPage).isNotEmpty();
			mockMvc.perform(get(Routes.PROFILE)
					.param("name", NAME_SEARCH_SILVA)
					.param("limit", "50")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content.length()").value(firstPage.size()))
					.andExpect(jsonPath("$.content[0].id").value(firstPage.get(0).getId().toString()));
		}

		@Test
		@DisplayName("Busca name=Silva e reverse=true: alinha a findByNameContainingIgnoreCaseDescending(50) no banco")
		void nameSearchWithReverseTrueAligns() throws Exception {
			final var firstPage = profileRepository.findByNameContainingIgnoreCaseDescending(50, NAME_SEARCH_SILVA);
			assertThat(firstPage).isNotEmpty();
			mockMvc.perform(get(Routes.PROFILE)
					.param("name", NAME_SEARCH_SILVA)
					.param("limit", "50")
					.param("reverse", "true")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.reverse").value(true))
					.andExpect(jsonPath("$.content.length()").value(firstPage.size()))
					.andExpect(jsonPath("$.content[0].id").value(firstPage.get(0).getId().toString()));
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
			final var mvcPage1 = mockMvc.perform(get(Routes.PROFILE)
					.param("name", NAME_SEARCH_QUEIROZ)
					.param("limit", String.valueOf(pageSize))
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.reverse").value(false))
					.andExpect(jsonPath("$.content.length()").value(pageSize))
					.andExpect(jsonPath("$.content[0].id").value(page1.getFirst().getId().toString()))
					.andExpect(jsonPath("$.content[1].id").value(page1.get(1).getId().toString()))
					.andReturn();
			assertThat(mvcPage1.getResponse().getContentAsString())
					.as("cada bloco de página deve anunciar ocorrências após o último item")
					.contains("\"followingElements\"");
			mockMvc.perform(get(Routes.PROFILE)
					.param("name", NAME_SEARCH_QUEIROZ)
					.param("limit", String.valueOf(pageSize))
					.param("lastSeenName", last1.getName())
					.param("lastSeenId", last1.getId().toString())
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content.length()").value(page2FromRepo.size()))
					.andExpect(jsonPath("$.content[0].id").value(page2FromRepo.getFirst().getId().toString()));
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
			final var mvcPage1 = mockMvc.perform(get(Routes.PROFILE)
					.param("name", NAME_SEARCH_QUEIROZ)
					.param("limit", String.valueOf(pageSize))
					.param("reverse", "true")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.reverse").value(true))
					.andExpect(jsonPath("$.content.length()").value(pageSize))
					.andExpect(jsonPath("$.content[0].id").value(page1.getFirst().getId().toString()))
					.andExpect(jsonPath("$.content[1].id").value(page1.get(1).getId().toString()))
					.andReturn();
			assertThat(mvcPage1.getResponse().getContentAsString())
					.as("cada bloco de página deve anunciar ocorrências após o cursor na ordenação desc")
					.contains("\"followingElements\"");
			mockMvc.perform(get(Routes.PROFILE)
					.param("name", NAME_SEARCH_QUEIROZ)
					.param("limit", String.valueOf(pageSize))
					.param("reverse", "true")
					.param("lastSeenName", last1.getName())
					.param("lastSeenId", last1.getId().toString())
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content.length()").value(page2FromRepo.size()))
					.andExpect(jsonPath("$.content[0].id").value(page2FromRepo.getFirst().getId().toString()));
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
				final var expectId = allMatches.get(i).getId().toString();
				mockMvc.perform(request)
						.andExpect(status().isOk())
						.andExpect(jsonPath("$.content.length()").value(1))
						.andExpect(jsonPath("$.content[0].id").value(expectId));
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
				final var expectId = allMatches.get(i).getId().toString();
				mockMvc.perform(request)
						.andExpect(status().isOk())
						.andExpect(jsonPath("$.reverse").value(true))
						.andExpect(jsonPath("$.content.length()").value(1))
						.andExpect(jsonPath("$.content[0].id").value(expectId));
			}
		}

		@Test
		@DisplayName("Busca por nome (reverse): segunda página descendente bate com o repositório")
		void nameSearchDescendingSecondPage() throws Exception {
			final int pageSize = 2;
			final var page1 = profileRepository.findByNameContainingIgnoreCaseDescending(pageSize, NAME_SEARCH_QUEIROZ);
			assertThat(page1).hasSize(pageSize);
			final var last1 = page1.getLast();
			final var page2 = profileRepository.findByNameContainingIgnoreCaseDescendingAfter(pageSize, last1.getName(),
					last1.getId(), NAME_SEARCH_QUEIROZ);
			assertThat(page2)
					.as("deve existir continuação na ordenação desc para testar o cursor (substring '%s')", NAME_SEARCH_QUEIROZ)
					.isNotEmpty();
			mockMvc.perform(get(Routes.PROFILE)
					.param("name", NAME_SEARCH_QUEIROZ)
					.param("limit", String.valueOf(pageSize))
					.param("reverse", "true")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.reverse").value(true))
					.andExpect(jsonPath("$.content[1].id").value(last1.getId().toString()));
			mockMvc.perform(get(Routes.PROFILE)
					.param("name", NAME_SEARCH_QUEIROZ)
					.param("limit", String.valueOf(pageSize))
					.param("reverse", "true")
					.param("lastSeenName", last1.getName())
					.param("lastSeenId", last1.getId().toString())
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content[0].id").value(page2.getFirst().getId().toString()));
		}

		@Test
		@DisplayName("Continuação de cursor: segunda página (limit=1) após o primeiro da ordering asc")
		void secondPageByCursor() throws Exception {
			final var first = profileRepository.findAllAscending(1);
			assertThat(first).hasSize(1);
			final var secondPage = profileRepository.findAllAscendingAfter(1, first.getFirst().getName(),
					first.getFirst().getId());
			assertThat(secondPage).hasSize(1);
			mockMvc.perform(get(Routes.PROFILE)
					.param("limit", "1")
					.param("lastSeenName", first.getFirst().getName())
					.param("lastSeenId", first.getFirst().getId().toString())
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content[0].id").value(secondPage.getFirst().getId().toString()));
		}

		@Test
		@DisplayName("Continuação de cursor (reverse): segunda página (limit=1) após o primeiro da ordering desc")
		void secondPageByCursorWithReverseTrue() throws Exception {
			final var first = profileRepository.findAllDescending(1);
			assertThat(first).hasSize(1);
			final var secondPage = profileRepository.findAllDescendingAfter(1, first.getFirst().getName(),
					first.getFirst().getId());
			assertThat(secondPage).hasSize(1);
			mockMvc.perform(get(Routes.PROFILE)
					.param("limit", "1")
					.param("reverse", "true")
					.param("lastSeenName", first.getFirst().getName())
					.param("lastSeenId", first.getFirst().getId().toString())
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.reverse").value(true))
					.andExpect(jsonPath("$.content[0].id").value(secondPage.getFirst().getId().toString()));
		}

		@Test
		@DisplayName("400 quando limit viola @Limit (0)")
		void badRequestWhenLimitInvalid() throws Exception {
			mockMvc.perform(get(Routes.PROFILE).param("limit", "0").accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.limit[0]")
							.value("deve ser um número positivo menor ou igual à 100"));
		}

		@Test
		@DisplayName("400 quando limit=0 e reverse=true (validação de limit inalterada)")
		void badRequestWhenLimitInvalidWithReverseTrue() throws Exception {
			mockMvc.perform(get(Routes.PROFILE).param("limit", "0").param("reverse", "true")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.limit[0]")
							.value("deve ser um número positivo menor ou igual à 100"));
		}

		@Test
		@DisplayName("400 quando limit excede o máximo (101)")
		void badRequestWhenLimitExceedsMax() throws Exception {
			mockMvc.perform(get(Routes.PROFILE).param("limit", "101").accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.limit[0]")
							.value("deve ser um número positivo menor ou igual à 100"));
		}

		@Test
		@DisplayName("400 quando limit=101 e reverse=true (validação de limit inalterada)")
		void badRequestWhenLimitExceedsMaxWithReverseTrue() throws Exception {
			mockMvc.perform(get(Routes.PROFILE).param("limit", "101").param("reverse", "true")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.limit[0]")
							.value("deve ser um número positivo menor ou igual à 100"));
		}

		@Test
		@DisplayName("name só com espaços: hasText=false; controlador aplica listagem geral (como local)")
		void whitespaceOnlyNameFallsBackToListAll() throws Exception {
			final var expected = profileRepository.findAllAscending(20);
			assertThat(expected).isNotEmpty();
			mockMvc.perform(get(Routes.PROFILE).param("name", "   ").param("limit", "20")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content[0].id").value(expected.get(0).getId().toString()));
		}

		@Test
		@DisplayName("name só com espaços e reverse=true: listagem geral desc (hasText ainda falso)")
		void whitespaceOnlyNameFallsBackToListAllWithReverseTrue() throws Exception {
			final var expected = profileRepository.findAllDescending(20);
			assertThat(expected).isNotEmpty();
			mockMvc.perform(get(Routes.PROFILE).param("name", "   ").param("limit", "20")
					.param("reverse", "true")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.reverse").value(true))
					.andExpect(jsonPath("$.content[0].id").value(expected.get(0).getId().toString()));
		}

		@Test
		@DisplayName("400 lastSeenName inválido (@Name) com busca por nome e cursor")
		void badRequestWhenLastSeenNameInvalid() throws Exception {
			mockMvc.perform(get(Routes.PROFILE)
					.param("name", NAME_SEARCH_SILVA)
					.param("lastSeenName", "Maria@Silva")
					.param("lastSeenId", ALICE_ID.toString())
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.lastSeenName[0]").value("deve ser um nome bem formado"));
		}

		@Test
		@DisplayName("400 lastSeenName inválido com busca, cursor e reverse=true")
		void badRequestWhenLastSeenNameInvalidWithReverseTrue() throws Exception {
			mockMvc.perform(get(Routes.PROFILE)
					.param("name", NAME_SEARCH_SILVA)
					.param("lastSeenName", "Maria@Silva")
					.param("lastSeenId", ALICE_ID.toString())
					.param("reverse", "true")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.lastSeenName[0]").value("deve ser um nome bem formado"));
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
			assertThat(result.getResponse().getContentAsString()).isEmpty();
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
			assertThat(result.getResponse().getContentAsString()).isEmpty();
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
			assertThat(result.getResponse().getContentAsString()).isEmpty();
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
			assertThat(result.getResponse().getContentAsString()).isEmpty();
		}
	}
}
