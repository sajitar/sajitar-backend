package com.sajitar.backend.adapter.in.web.controller.profile;

import static com.sajitar.backend.settlement.profile.ProfileSettlementFixture.ALICE_BIRTHDAY;
import static com.sajitar.backend.settlement.profile.ProfileSettlementFixture.ALICE_DESCRIPTION;
import static com.sajitar.backend.settlement.profile.ProfileSettlementFixture.ALICE_EMAIL;
import static com.sajitar.backend.settlement.profile.ProfileSettlementFixture.ALICE_ID;
import static com.sajitar.backend.settlement.profile.ProfileSettlementFixture.ALICE_NAME;
import static com.sajitar.backend.settlement.profile.ProfileSettlementFixture.BRUNO_ID;
import static com.sajitar.backend.settlement.profile.ProfileSettlementFixture.CARLA_ID;
import static com.sajitar.backend.settlement.profile.ProfileSettlementFixture.DANIEL_ID;
import static com.sajitar.backend.settlement.profile.ProfileSettlementFixture.NAME_SEARCH_NO_MATCH;
import static com.sajitar.backend.settlement.profile.ProfileSettlementFixture.NAME_SEARCH_QUEIROZ;
import static com.sajitar.backend.settlement.profile.ProfileSettlementFixture.NAME_SEARCH_SILVA;
import static com.sajitar.backend.settlement.profile.ProfileSettlementFixture.PASSWORD_HASH;
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
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sajitar.backend.adapter.in.web.Routes;
import com.sajitar.backend.adapter.in.web.controller.IntegrationAuth;
import com.sajitar.backend.adapter.out.mail.RecordingMailer;
import com.sajitar.backend.adapter.out.persistence.checker.CheckerJpaEntity;
import com.sajitar.backend.adapter.out.persistence.checker.CheckerJpaRepository;
import com.sajitar.backend.adapter.out.persistence.profile.ProfileJpaEntity;
import com.sajitar.backend.adapter.out.persistence.profile.ProfileJpaRepository;
import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.model.profile.Profile;
import com.sajitar.backend.settlement.token.SessionSettlementFixture;

import jakarta.persistence.EntityManager;

/**
 * Integração do {@link ProfileController} com a massa
 * {@code classpath:settlement/profile.sql} (mesma cadeia que ambiente local:
 * funções, colunas, unicidades, índices e inserts — ver {@code src/test/resources/application.yml}).
 */
@SpringBootTest(properties = {
		"sajitar.security.attempt.credentials-max=5",
		"sajitar.security.attempt.credentials-window-seconds=60"
})
@DisplayName("ProfileController (integração HTTP + settlement)")
class ProfileControllerIntegrationTest {

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private ProfileJpaRepository profileRepository;

	@Autowired
	private CheckerJpaRepository checkerRepository;

	@Autowired
	private StringRedisTemplate redis;

	@Autowired
	private EntityManager entityManager;

	@Autowired(required = false)
	private RecordingMailer recordingMailer;

	/** Mesma leitura JSON da API; não depende de bean {@code ObjectMapper} no contexto. */
	private final ObjectMapper objectMapper = new ObjectMapper();

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		if (recordingMailer != null) {
			recordingMailer.clear();
		}
		SessionSettlementFixture.clear(redis);
		mockMvc = IntegrationAuth.withSecurityAndAliceBearer(webApplicationContext);
	}

	/** Primeira página (sem cursor): só {@code followingElements}; {@code precedingElements} permanece 0. */
	private static long findAllFollowingAfterLast(final ProfileJpaRepository repo, final List<ProfileJpaEntity> page,
			final boolean reverse) {
		final var last = page.getLast();
		return reverse ? repo.countForFindAllDescendingAfter(last.getName(), last.getId(), ALICE_ID)
				: repo.countForFindAllAscendingAfter(last.getName(), last.getId(), ALICE_ID);
	}

	/** Página de continuação: itens antes do primeiro (ordenação oposta), alinhado a {@code precedingElements}. */
	private static long findAllPrecedingAfterFirst(final ProfileJpaRepository repo, final List<ProfileJpaEntity> page,
			final boolean reverse) {
		final var first = page.getFirst();
		return reverse ? repo.countForFindAllAscendingAfter(first.getName(), first.getId(), ALICE_ID)
				: repo.countForFindAllDescendingAfter(first.getName(), first.getId(), ALICE_ID);
	}

	private static long nameSearchFollowingAfterLast(final ProfileJpaRepository repo, final List<ProfileJpaEntity> page,
			final boolean reverse, final String name) {
		final var last = page.getLast();
		return reverse
				? repo.countForFindByNameContainingIgnoreCaseDescendingAfter(last.getName(), last.getId(), name,
						ALICE_ID)
				: repo.countForFindByNameContainingIgnoreCaseAscendingAfter(last.getName(), last.getId(), name,
						ALICE_ID);
	}

	private static long nameSearchPrecedingAfterFirst(final ProfileJpaRepository repo, final List<ProfileJpaEntity> page,
			final boolean reverse, final String name) {
		final var first = page.getFirst();
		return reverse
				? repo.countForFindByNameContainingIgnoreCaseAscendingAfter(first.getName(), first.getId(), name,
						ALICE_ID)
				: repo.countForFindByNameContainingIgnoreCaseDescendingAfter(first.getName(), first.getId(), name,
						ALICE_ID);
	}

	private static String responseBodyUtf8(final MvcResult result) {
		return new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
	}

	private Set<String> jsonObjectKeys(final JsonNode node) {
		final var keys = new HashSet<String>();
		node.fieldNames().forEachRemaining(keys::add);
		return keys;
	}

	private List<String> contentIds(final MvcResult result) throws Exception {
		final JsonNode content = objectMapper.readTree(responseBodyUtf8(result)).get("content");
		final var ids = new ArrayList<String>();
		content.forEach(node -> ids.add(node.get("id").asText()));
		return ids;
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
		assertThat(jsonObjectKeys(node)).containsExactlyInAnyOrder("id", "type", "name", "description");
		assertThat(node.get("type").asText()).isEqualTo(expected.getType().name());
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
		@DisplayName("200, JSON com id, type, name e description (dados reais: Alice, settlement)")
		void returns200WithAlice() throws Exception {
			final ProfileJpaEntity alice = profileRepository.findById(ALICE_ID).orElseThrow();
			final MvcResult result = mockMvc.perform(get(Routes.PROFILE + "/" + ALICE_ID).accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andReturn();
			assertThat(result.getResponse().getContentType()).contains("json");
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactlyInAnyOrder("id", "type", "name", "description");
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
		@DisplayName("401 sem Bearer")
		void returns401WhenBearerIsMissing() throws Exception {
			final var anonymous = IntegrationAuth.withSecurity(webApplicationContext);
			final MvcResult result = anonymous.perform(get(Routes.PROFILE + "/" + ALICE_ID).accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isUnauthorized())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("token");
			assertThat(n.get("token").get(0).asText()).contains("bearer token");
		}

		@Test
		@DisplayName("401 com Bearer inválido")
		void returns401WhenBearerIsInvalid() throws Exception {
			final MvcResult result = mockMvc.perform(get(Routes.PROFILE + "/" + ALICE_ID)
					.header("Authorization", "Bearer not-a-jwt")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isUnauthorized())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("token");
			assertThat(n.get("token").get(0).asText()).contains("bearer token");
		}

		@Test
		@DisplayName("401 com refresh JWT no Authorization")
		void returns401WhenRefreshTokenIsUsedAsBearer() throws Exception {
			final var refresh = IntegrationAuth.danglingRefreshToken(webApplicationContext);
			final MvcResult result = mockMvc.perform(get(Routes.PROFILE + "/" + ALICE_ID)
					.header("Authorization", "Bearer " + refresh)
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isUnauthorized())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("token");
			assertThat(n.get("token").get(0).asText()).contains("bearer token");
		}

		@Test
		@DisplayName("401 com Authorization Basic em rota protegida")
		void returns401WhenBasicAuthorizationIsSent() throws Exception {
			final var anonymous = IntegrationAuth.withSecurity(webApplicationContext);
			final MvcResult result = anonymous.perform(get(Routes.PROFILE + "/" + ALICE_ID)
					.header("Authorization", "Basic dXNlcjpwYXNz")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isUnauthorized())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("token");
			assertThat(n.get("token").get(0).asText()).contains("bearer token");
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
			assertThat(jsonObjectKeys(n)).containsExactlyInAnyOrder("id", "type", "name", "description", "birthday", "email", "twoFactor");
			assertThat(n.get("id").asText()).isEqualTo(ALICE_ID.toString());
			assertThat(n.get("type").asText()).isEqualTo("MASTER");
			assertThat(n.get("name").asText()).isEqualTo(ALICE_NAME);
			assertThat(n.get("description").asText()).isEqualTo(ALICE_DESCRIPTION);
			assertThat(n.get("birthday").asText()).isEqualTo(ALICE_BIRTHDAY);
			assertThat(n.get("email").asText()).isEqualTo(ALICE_EMAIL);
			assertThat(n.get("twoFactor").asBoolean()).isTrue();
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

		@Test
		@DisplayName("GET /profiles/{id}/details da Carla no próprio id retorna 200")
		void carlaOwnDetailsReturns200() throws Exception {
			final var carla = IntegrationAuth.withSecurityAndBearer(webApplicationContext, CARLA_ID);
			final var expected = profileRepository.findById(CARLA_ID).orElseThrow();
			final MvcResult result = carla
					.perform(get(Routes.PROFILE + "/" + CARLA_ID + "/details").accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactlyInAnyOrder("id", "type", "name", "description", "birthday", "email", "twoFactor");
			assertThat(n.get("id").asText()).isEqualTo(CARLA_ID.toString());
			assertThat(n.get("email").asText()).isEqualTo(expected.getEmail());
			assertThat(n.get("birthday").asText()).isEqualTo(expected.getBirthday().toString());
		}

		@Test
		@DisplayName("GET /profiles/{id}/details da Alice na Carla retorna 200")
		void aliceReadsCarlaDetailsReturns200() throws Exception {
			final var expected = profileRepository.findById(CARLA_ID).orElseThrow();
			final MvcResult result = mockMvc
					.perform(get(Routes.PROFILE + "/" + CARLA_ID + "/details").accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(n.get("id").asText()).isEqualTo(CARLA_ID.toString());
			assertThat(n.get("email").asText()).isEqualTo(expected.getEmail());
		}

		@Test
		@DisplayName("GET /profiles/{id}/details da Alice é 403 para a Carla")
		void getAliceDetailsAsCarlaReturns403() throws Exception {
			final var carla = IntegrationAuth.withSecurityAndBearer(webApplicationContext, CARLA_ID);
			final var result = carla
					.perform(get(Routes.PROFILE + "/" + ALICE_ID + "/details").accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isForbidden())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("id");
			assertThat(n.get("id").get(0).asText()).contains("authenticated profile");
		}
	}

	@Nested
	@DisplayName("GET perfil com VERIFY_EMAIL (viewer sem MASTER)")
	class UnverifiedHiddenFromNonMaster {

		@Test
		@DisplayName("GET /profiles/{id} da Alice é 404 para a Carla")
		void getAliceAsCarlaReturns404() throws Exception {
			final var carla = IntegrationAuth.withSecurityAndBearer(webApplicationContext, CARLA_ID);
			final var result = carla.perform(get(Routes.PROFILE + "/" + ALICE_ID).accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}

		@Test
		@DisplayName("GET /profiles/{id} do Bruno é 200 para a Carla")
		void getBrunoAsCarlaReturns200() throws Exception {
			final var carla = IntegrationAuth.withSecurityAndBearer(webApplicationContext, CARLA_ID);
			final var bruno = profileRepository.findById(BRUNO_ID).orElseThrow();
			final MvcResult result = carla.perform(get(Routes.PROFILE + "/" + BRUNO_ID).accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertProfileSummaryNode(n, bruno);
		}
	}

	@Nested
	@DisplayName("GET perfil READER (viewer sem MASTER)")
	class ReaderHiddenFromNonMaster {

		@Test
		@DisplayName("GET /profiles/{id} da Carla no próprio id retorna 200")
		void getCarlaAsCarlaReturns200() throws Exception {
			final var carla = IntegrationAuth.withSecurityAndBearer(webApplicationContext, CARLA_ID);
			final var expected = profileRepository.findById(CARLA_ID).orElseThrow();
			final MvcResult result = carla.perform(get(Routes.PROFILE + "/" + CARLA_ID).accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertProfileSummaryNode(n, expected);
		}

		@Test
		@DisplayName("GET /profiles/{id} do Daniel é 404 para a Carla")
		void getDanielAsCarlaReturns404() throws Exception {
			final var carla = IntegrationAuth.withSecurityAndBearer(webApplicationContext, CARLA_ID);
			final var result = carla.perform(get(Routes.PROFILE + "/" + DANIEL_ID).accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}

		@Test
		@DisplayName("GET /profiles/{id} da Carla é 200 para a Alice")
		void getCarlaAsAliceReturns200() throws Exception {
			final var expected = profileRepository.findById(CARLA_ID).orElseThrow();
			final MvcResult result = mockMvc.perform(get(Routes.PROFILE + "/" + CARLA_ID).accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertProfileSummaryNode(n, expected);
		}

		@Test
		@DisplayName("GET /profiles da Carla omite o próprio perfil, Alice e outros READER e lista Bruno com os WRITER")
		void listAsCarlaOmitsAlice() throws Exception {
			final var carla = IntegrationAuth.withSecurityAndBearer(webApplicationContext, CARLA_ID);
			final var verifyEmail = (short) Checker.Type.VERIFY_EMAIL.value();
			final var reader = (short) Profile.Type.READER.value();
			final var expected = profileRepository.findAllAscending(
					100,
					false,
					verifyEmail,
					false,
					reader,
					CARLA_ID,
					true,
					(short) 0,
					true,
					false);
			assertThat(expected).hasSize(100);
			assertThat(expected).noneMatch(profile -> profile.getId().equals(ALICE_ID));
			assertThat(expected).noneMatch(profile -> profile.getId().equals(DANIEL_ID));
			assertThat(expected).noneMatch(profile -> profile.getId().equals(CARLA_ID));
			assertThat(expected).anyMatch(profile -> profile.getId().equals(BRUNO_ID));
			final var last = expected.getLast();
			final long following = profileRepository.countForFindAllAscendingAfter(
					last.getName(),
					last.getId(),
					false,
					verifyEmail,
					false,
					reader,
					CARLA_ID,
					true,
					(short) 0,
					true,
					false);
			assertThat(following).isEqualTo(24);
			final MvcResult result = carla.perform(get(Routes.PROFILE)
					.param("limit", "100")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			assertPaginationMvcResult(result, expected, false, 0, following);
		}

		@Test
		@DisplayName("GET /profiles da Carla com type=WRITER retorna 403 {type}")
		void listAsCarlaWithTypeReturns403() throws Exception {
			final var carla = IntegrationAuth.withSecurityAndBearer(webApplicationContext, CARLA_ID);
			final MvcResult result = carla.perform(get(Routes.PROFILE)
					.param("type", "WRITER")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isForbidden())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("type");
			assertThat(n.get("type").get(0).asText()).contains("assigned by a master");
		}

		@Test
		@DisplayName("GET /profiles da Carla com verified=true retorna 403 {verified}")
		void listAsCarlaWithVerifiedReturns403() throws Exception {
			final var carla = IntegrationAuth.withSecurityAndBearer(webApplicationContext, CARLA_ID);
			final MvcResult result = carla.perform(get(Routes.PROFILE)
					.param("verified", "true")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isForbidden())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("verified");
			assertThat(n.get("verified").get(0).asText()).contains("requested by a master");
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
			final var expected = profileRepository.findAllAscending(100, ALICE_ID);
			assertThat(expected).hasSize(100);
			assertThat(expected).noneMatch(profile -> profile.getId().equals(ALICE_ID));
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
			final var expected = profileRepository.findAllDescending(100, ALICE_ID);
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
			final var five = profileRepository.findAllAscending(5, ALICE_ID);
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
			final var five = profileRepository.findAllDescending(5, ALICE_ID);
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
			final var desc = profileRepository.findAllDescending(3, ALICE_ID);
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
			final var firstPage = profileRepository.findByNameContainingIgnoreCaseAscending(50, NAME_SEARCH_SILVA, ALICE_ID);
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
			final var firstPage = profileRepository.findByNameContainingIgnoreCaseDescending(50, NAME_SEARCH_SILVA, ALICE_ID);
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
			final var allMatches = profileRepository.findByNameContainingIgnoreCaseAscending(500, NAME_SEARCH_QUEIROZ, ALICE_ID);
			assertThat(allMatches)
					.as("settlement: ao menos 3 ocorrências de '%s' (ex.: família Queiroz no script)", NAME_SEARCH_QUEIROZ)
					.hasSizeGreaterThanOrEqualTo(3);
			final int pageSize = 2;
			final var page1 = profileRepository.findByNameContainingIgnoreCaseAscending(pageSize, NAME_SEARCH_QUEIROZ, ALICE_ID);
			assertThat(page1).hasSize(pageSize);
			final var last1 = page1.getLast();
			final var page2FromRepo = profileRepository.findByNameContainingIgnoreCaseAscendingAfter(pageSize, last1.getName(),
					last1.getId(), NAME_SEARCH_QUEIROZ, ALICE_ID);
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
			final var allMatches = profileRepository.findByNameContainingIgnoreCaseDescending(500, NAME_SEARCH_QUEIROZ, ALICE_ID);
			assertThat(allMatches)
					.as("settlement: ao menos 3 ocorrências de '%s' (ex.: família Queiroz no script)", NAME_SEARCH_QUEIROZ)
					.hasSizeGreaterThanOrEqualTo(3);
			final int pageSize = 2;
			final var page1 = profileRepository.findByNameContainingIgnoreCaseDescending(pageSize, NAME_SEARCH_QUEIROZ, ALICE_ID);
			assertThat(page1).hasSize(pageSize);
			final var last1 = page1.getLast();
			final var page2FromRepo = profileRepository.findByNameContainingIgnoreCaseDescendingAfter(pageSize, last1.getName(),
					last1.getId(), NAME_SEARCH_QUEIROZ, ALICE_ID);
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
			final var allMatches = profileRepository.findByNameContainingIgnoreCaseAscending(500, NAME_SEARCH_QUEIROZ, ALICE_ID);
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
			final var allMatches = profileRepository.findByNameContainingIgnoreCaseDescending(500, NAME_SEARCH_QUEIROZ, ALICE_ID);
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
			final var first = profileRepository.findAllAscending(1, ALICE_ID);
			assertThat(first).hasSize(1);
			final var secondPage = profileRepository.findAllAscendingAfter(1, first.getFirst().getName(),
					first.getFirst().getId(), ALICE_ID);
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
			final var first = profileRepository.findAllDescending(1, ALICE_ID);
			assertThat(first).hasSize(1);
			final var secondPage = profileRepository.findAllDescendingAfter(1, first.getFirst().getName(),
					first.getFirst().getId(), ALICE_ID);
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
			final var expected = profileRepository.findAllAscending(20, ALICE_ID);
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
			final var expected = profileRepository.findAllDescending(20, ALICE_ID);
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
			final var cursorName = profileRepository.findByNameContainingIgnoreCaseAscending(1, NAME_SEARCH_SILVA, ALICE_ID)
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
			final var anchor = profileRepository.findAllAscending(1, ALICE_ID).getFirst();
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
			final var last = profileRepository.findAllAscending(1_000, ALICE_ID).getLast();
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
			final var last = profileRepository.findAllDescending(1_000, ALICE_ID).getLast();
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
			final var matches = profileRepository.findByNameContainingIgnoreCaseAscending(500, NAME_SEARCH_QUEIROZ, ALICE_ID);
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
			final var matches = profileRepository.findByNameContainingIgnoreCaseDescending(500, NAME_SEARCH_QUEIROZ, ALICE_ID);
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
				assertThat(profileRepository.findByNameContainingIgnoreCaseDescending(50, NAME_SEARCH_NO_MATCH, ALICE_ID)).isEmpty();
			} else {
				assertThat(profileRepository.findByNameContainingIgnoreCaseAscending(50, NAME_SEARCH_NO_MATCH, ALICE_ID)).isEmpty();
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
			final var expected = profileRepository.findAllAscending(15, ALICE_ID);
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
			final var expected = profileRepository.findAllDescending(15, ALICE_ID);
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
			final var expected = profileRepository.findAllAscending(7, ALICE_ID);
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
			final var expected = profileRepository.findAllDescending(7, ALICE_ID);
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
			final var expected = profileRepository.findAllAscending(6, ALICE_ID);
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
			final var expected = profileRepository.findAllDescending(6, ALICE_ID);
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
			final var expected = profileRepository.findByNameContainingIgnoreCaseAscending(8, NAME_SEARCH_SILVA, ALICE_ID);
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
			final var expected = profileRepository.findByNameContainingIgnoreCaseDescending(8, NAME_SEARCH_SILVA, ALICE_ID);
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
			final var expected = profileRepository.findByNameContainingIgnoreCaseAscending(8, NAME_SEARCH_QUEIROZ, ALICE_ID);
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
			final var expected = profileRepository.findByNameContainingIgnoreCaseDescending(8, NAME_SEARCH_QUEIROZ, ALICE_ID);
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
			final var expected = profileRepository.findAllAscending(4, ALICE_ID);
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

		@Test
		@DisplayName("type=MASTER: só Bruno (Alice omitida) e following 0")
		void typeMasterReturnsOnlyBruno() throws Exception {
			final var verifyEmail = (short) Checker.Type.VERIFY_EMAIL.value();
			final var reader = (short) Profile.Type.READER.value();
			final var master = (short) Profile.Type.MASTER.value();
			final var expected = profileRepository.findAllAscending(
					10,
					true,
					verifyEmail,
					true,
					reader,
					ALICE_ID,
					false,
					master,
					true,
					false);
			assertThat(expected).hasSize(1);
			assertThat(expected.getFirst().getId()).isEqualTo(BRUNO_ID);
			assertThat(expected).noneMatch(profile -> profile.getId().equals(ALICE_ID));
			final var last = expected.getLast();
			final long following = profileRepository.countForFindAllAscendingAfter(
					last.getName(),
					last.getId(),
					true,
					verifyEmail,
					true,
					reader,
					ALICE_ID,
					false,
					master,
					true,
					false);
			assertThat(following).isZero();
			final MvcResult result = mockMvc.perform(get(Routes.PROFILE)
					.param("type", "MASTER")
					.param("limit", "10")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			assertPaginationMvcResult(result, expected, false, 0, following);
		}

		@Test
		@DisplayName("type=READER: content só READER e alinhado ao repositório")
		void typeReaderAlignsWithRepository() throws Exception {
			final var verifyEmail = (short) Checker.Type.VERIFY_EMAIL.value();
			final var reader = (short) Profile.Type.READER.value();
			final var expected = profileRepository.findAllAscending(
					10,
					true,
					verifyEmail,
					true,
					reader,
					ALICE_ID,
					false,
					reader,
					true,
					false);
			assertThat(expected).isNotEmpty();
			assertThat(expected).allMatch(profile -> profile.getType() == Profile.Type.READER);
			final var last = expected.getLast();
			final long following = profileRepository.countForFindAllAscendingAfter(
					last.getName(),
					last.getId(),
					true,
					verifyEmail,
					true,
					reader,
					ALICE_ID,
					false,
					reader,
					true,
					false);
			final MvcResult result = mockMvc.perform(get(Routes.PROFILE)
					.param("type", "READER")
					.param("limit", "10")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			assertPaginationMvcResult(result, expected, false, 0, following);
		}

		@Test
		@DisplayName("type=WRITER: content só WRITER e alinhado ao repositório")
		void typeWriterAlignsWithRepository() throws Exception {
			final var verifyEmail = (short) Checker.Type.VERIFY_EMAIL.value();
			final var reader = (short) Profile.Type.READER.value();
			final var writer = (short) Profile.Type.WRITER.value();
			final var expected = profileRepository.findAllAscending(
					10,
					true,
					verifyEmail,
					true,
					reader,
					ALICE_ID,
					false,
					writer,
					true,
					false);
			assertThat(expected).hasSize(10);
			assertThat(expected).allMatch(profile -> profile.getType() == Profile.Type.WRITER);
			final var last = expected.getLast();
			final long following = profileRepository.countForFindAllAscendingAfter(
					last.getName(),
					last.getId(),
					true,
					verifyEmail,
					true,
					reader,
					ALICE_ID,
					false,
					writer,
					true,
					false);
			assertThat(following).isEqualTo(113);
			final MvcResult result = mockMvc.perform(get(Routes.PROFILE)
					.param("type", "WRITER")
					.param("limit", "10")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			assertPaginationMvcResult(result, expected, false, 0, following);
		}

		@ParameterizedTest(name = "type={0}")
		@ValueSource(strings = { "MEMBER", "4" })
		@DisplayName("400 type inválido")
		void typeInvalidReturns400(final String type) throws Exception {
			final MvcResult br = mockMvc.perform(get(Routes.PROFILE)
					.param("type", type)
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(br, "type", "Profile.Type");
		}

		@Test
		@DisplayName("type só com espaços: hasText=false; listagem geral")
		void whitespaceOnlyTypeFallsBackToListAll() throws Exception {
			final var expected = profileRepository.findAllAscending(20, ALICE_ID);
			assertThat(expected).isNotEmpty();
			final long following = findAllFollowingAfterLast(profileRepository, expected, false);
			final MvcResult result = mockMvc.perform(get(Routes.PROFILE)
					.param("type", "   ")
					.param("limit", "20")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			assertPaginationMvcResult(result, expected, false, 0, following);
		}

		@Test
		@DisplayName("verified=true: omite VERIFY_EMAIL e alinhado ao repositório")
		void verifiedTrueAlignsWithRepository() throws Exception {
			final var verifyEmail = (short) Checker.Type.VERIFY_EMAIL.value();
			final var reader = (short) Profile.Type.READER.value();
			final var expected = profileRepository.findAllAscending(
					10,
					true,
					verifyEmail,
					true,
					reader,
					ALICE_ID,
					true,
					(short) 0,
					false,
					true);
			assertThat(expected).isNotEmpty();
			assertThat(expected).noneMatch(profile -> profile.getId().equals(ALICE_ID));
			final var last = expected.getLast();
			final long following = profileRepository.countForFindAllAscendingAfter(
					last.getName(),
					last.getId(),
					true,
					verifyEmail,
					true,
					reader,
					ALICE_ID,
					true,
					(short) 0,
					false,
					true);
			final MvcResult result = mockMvc.perform(get(Routes.PROFILE)
					.param("verified", "true")
					.param("limit", "10")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			assertPaginationMvcResult(result, expected, false, 0, following);
		}

		@Test
		@DisplayName("verified=false da Alice: 404 (único VERIFY_EMAIL é o viewer)")
		void verifiedFalseAsAliceReturns404() throws Exception {
			final var result = mockMvc.perform(get(Routes.PROFILE)
					.param("verified", "false")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}

		@Test
		@DisplayName("verified=false da Bruno: só Alice e following 0")
		void verifiedFalseAsBrunoReturnsOnlyAlice() throws Exception {
			final var bruno = IntegrationAuth.withSecurityAndBearer(webApplicationContext, BRUNO_ID);
			final var verifyEmail = (short) Checker.Type.VERIFY_EMAIL.value();
			final var reader = (short) Profile.Type.READER.value();
			final var expected = profileRepository.findAllAscending(
					10,
					true,
					verifyEmail,
					true,
					reader,
					BRUNO_ID,
					true,
					(short) 0,
					false,
					false);
			assertThat(expected).hasSize(1);
			assertThat(expected.getFirst().getId()).isEqualTo(ALICE_ID);
			final var last = expected.getLast();
			final long following = profileRepository.countForFindAllAscendingAfter(
					last.getName(),
					last.getId(),
					true,
					verifyEmail,
					true,
					reader,
					BRUNO_ID,
					true,
					(short) 0,
					false,
					false);
			assertThat(following).isZero();
			final MvcResult result = bruno.perform(get(Routes.PROFILE)
					.param("verified", "false")
					.param("limit", "10")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			assertPaginationMvcResult(result, expected, false, 0, following);
		}

		@Test
		@DisplayName("400 quando verified não é booleano válido")
		void verifiedInvalidReturns400() throws Exception {
			final MvcResult br = mockMvc
					.perform(get(Routes.PROFILE).param("verified", "talvez").accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(br, "verified", "belong", "type");
		}
	}

	@Nested
	@Transactional
	@DisplayName("POST/PUT/PATCH/DELETE /profiles")
	class WriteProfiles {

		@Test
		@DisplayName("POST persiste senha em BCrypt e não devolve a senha no JSON")
		void postHashesPasswordAndOmitsItFromResponse() throws Exception {
			final MvcResult result = mockMvc.perform(post(Routes.PROFILE)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "type": "READER",
							  "name": "Zaida Nova",
							  "description": "Perfil criado no teste de integração.",
							  "birthday": "1990-01-01",
							  "email": "zaida.nova@example.com",
							  "password": "senhaSegura1",
							  "twoFactor": true
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactlyInAnyOrder("id", "type", "name", "description");
			assertThat(n.get("name").asText()).isEqualTo("Zaida Nova");
			assertThat(n.get("type").asText()).isEqualTo("READER");
			final var persisted = profileRepository.findByEmail("zaida.nova@example.com").orElseThrow();
			assertThat(persisted.getPassword()).startsWith("$2a$");
			assertThat(persisted.getType().name()).isEqualTo("READER");
			assertThat(persisted.getPassword()).isNotEqualTo("senhaSegura1");
			assertThat(persisted.getPassword()).hasSize(60);
			assertThat(persisted.isTwoFactor()).isFalse();
			assertThat(checkerRepository.findByProfileIdAndType(persisted.getId(), Checker.Type.SIGN_IN)).isEmpty();
			final var verifyEmail = checkerRepository
					.findByProfileIdAndType(persisted.getId(), Checker.Type.VERIFY_EMAIL)
					.orElseThrow();
			assertThat(verifyEmail.getCode()).matches("^[0-9]{6}$");
			assertThat(verifyEmail.getPayload()).isNull();
			if (recordingMailer != null) {
				assertThat(recordingMailer.sent()).hasSize(1);
				final var mail = recordingMailer.sent().getFirst();
				assertThat(mail.to()).isEqualTo("zaida.nova@example.com");
				assertThat(mail.subject()).startsWith("Your Sajitar code · ");
				assertThat(mail.subject()).containsPattern("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2} UTC");
				assertThat(mail.subject()).doesNotContain(verifyEmail.getCode());
				assertThat(mail.body()).contains("<title>" + mail.subject() + "</title>");
				assertThat(mail.body()).contains(verifyEmail.getCode());
			}
		}

		@Test
		@DisplayName("POST não verificado é 404 para a Carla e 200 para a Alice")
		void postUnverifiedIsHiddenFromCarlaAndVisibleToAlice() throws Exception {
			final MvcResult created = mockMvc.perform(post(Routes.PROFILE)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "type": "READER",
							  "name": "Zaida Nova",
							  "description": "Perfil criado no teste de integração.",
							  "birthday": "1990-01-01",
							  "email": "zaida.hidden@example.com",
							  "password": "senhaSegura1"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final var id = objectMapper.readTree(responseBodyUtf8(created)).get("id").asText();
			final var carla = IntegrationAuth.withSecurityAndBearer(webApplicationContext, CARLA_ID);
			final var hidden = carla.perform(get(Routes.PROFILE + "/" + id).accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(hidden);
			mockMvc.perform(get(Routes.PROFILE + "/" + id).accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk());
		}

		@Test
		@DisplayName("POST não verificado: Alice verified=true omite e verified=false inclui")
		void postUnverifiedAppearsOnlyWhenVerifiedFalse() throws Exception {
			final MvcResult created = mockMvc.perform(post(Routes.PROFILE)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "type": "READER",
							  "name": "Zaida Filtro",
							  "description": "Perfil criado no teste de integração.",
							  "birthday": "1990-01-01",
							  "email": "zaida.verified.filter@example.com",
							  "password": "senhaSegura1"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final var id = objectMapper.readTree(responseBodyUtf8(created)).get("id").asText();
			final MvcResult verifiedTrue = mockMvc.perform(get(Routes.PROFILE)
					.param("name", "Zaida Filtro")
					.param("verified", "true")
					.param("limit", "100")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(verifiedTrue);
			final MvcResult verifiedFalse = mockMvc.perform(get(Routes.PROFILE)
					.param("name", "Zaida Filtro")
					.param("verified", "false")
					.param("limit", "100")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			assertThat(contentIds(verifiedFalse)).containsExactly(id);
			final MvcResult omitted = mockMvc.perform(get(Routes.PROFILE)
					.param("name", "Zaida Filtro")
					.param("limit", "100")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			assertThat(contentIds(omitted)).containsExactly(id);
		}

		@Test
		@DisplayName("POST com e-mail já registrado retorna 409")
		void postDuplicateEmailReturns409() throws Exception {
			final MvcResult result = mockMvc.perform(post(Routes.PROFILE)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "type": "READER",
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
							  "type": "READER",
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
							  "type": "READER",
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
							  "type": "READER",
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
							  "type": "MASTER",
							  "name": "Alice Alves",
							  "description": "Descrição atualizada no teste.",
							  "birthday": "1988-01-10",
							  "email": "alice@example.com",
							  "twoFactor": true
							}""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(n.get("description").asText()).isEqualTo("Descrição atualizada no teste.");
			final var persisted = profileRepository.findById(ALICE_ID).orElseThrow();
			assertThat(persisted.getPassword()).isEqualTo(PASSWORD_HASH);
			assertThat(persisted.getDescription()).isEqualTo("Descrição atualizada no teste.");
		}

		@Test
		@DisplayName("PUT ignora password extra e mantém o hash")
		void putIgnoresUnknownPasswordField() throws Exception {
			mockMvc.perform(put(Routes.PROFILE + "/" + ALICE_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "type": "MASTER",
							  "name": "Alice Alves",
							  "description": "Uma pessoa criativa e dedicada.",
							  "birthday": "1988-01-10",
							  "email": "alice@example.com",
							  "password": "novaSenhaSegura1",
							  "twoFactor": true
							}""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk());
			final var persisted = profileRepository.findById(ALICE_ID).orElseThrow();
			assertThat(persisted.getPassword()).isEqualTo(PASSWORD_HASH);
		}

		@Test
		@DisplayName("PUT ignora e-mail extra e mantém o vigente")
		void putIgnoresUnknownEmailField() throws Exception {
			final MvcResult result = mockMvc.perform(put(Routes.PROFILE + "/" + ALICE_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "type": "MASTER",
							  "name": "Alice Alves",
							  "description": "Uma pessoa criativa e dedicada.",
							  "birthday": "1988-01-10",
							  "email": "bruno@example.com",
							  "twoFactor": true
							}""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(n.get("id").asText()).isEqualTo(ALICE_ID.toString());
			final var alice = profileRepository.findById(ALICE_ID).orElseThrow();
			assertThat(alice.getEmail()).isEqualTo(ALICE_EMAIL);
		}

		@Test
		@DisplayName("PUT com id inexistente retorna 404 sem corpo")
		void putUnknownIdReturns404() throws Exception {
			final var result = mockMvc.perform(put(Routes.PROFILE + "/" + UNKNOWN_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "type": "MASTER",
							  "name": "Ninguem Existe",
							  "description": "x",
							  "birthday": "1988-01-10",
							  "email": "ninguem@example.com",
							  "twoFactor": true
							}""")
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
							  "type": "MASTER",
							  "name": "Alice Alves",
							  "description": "Uma pessoa criativa e dedicada.",
							  "birthday": "1988-01-10",
							  "email": "alice@example.com",
							  "twoFactor": true
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
							}""")
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
		@DisplayName("PATCH ignora e-mail extra e mantém o vigente")
		void patchIgnoresUnknownEmailField() throws Exception {
			final MvcResult result = mockMvc.perform(patch(Routes.PROFILE + "/" + ALICE_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "email": "bruno@example.com"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(n.get("id").asText()).isEqualTo(ALICE_ID.toString());
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
			assertThat(persisted.getPassword()).isEqualTo(PASSWORD_HASH);
			assertThat(persisted.getType()).isEqualTo(com.sajitar.backend.domain.model.profile.Profile.Type.MASTER);
		}

		@Test
		@DisplayName("POST sem type grava WRITER")
		void postMissingTypeDefaultsToWriter() throws Exception {
			final MvcResult result = mockMvc.perform(post(Routes.PROFILE)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name": "Zaida Nova",
							  "description": "Perfil criado no teste de integração.",
							  "birthday": "1990-01-01",
							  "email": "zaida.semtipo@example.com",
							  "password": "senhaSegura1"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(n.get("type").asText()).isEqualTo("WRITER");
			assertThat(profileRepository.findByEmail("zaida.semtipo@example.com").orElseThrow().getType())
					.isEqualTo(com.sajitar.backend.domain.model.profile.Profile.Type.WRITER);
		}

		@Test
		@DisplayName("POST sem Bearer ignora type e grava WRITER")
		void postWithoutBearerIgnoresTypeAndDefaultsToWriter() throws Exception {
			final var anonymous = IntegrationAuth.withSecurity(webApplicationContext);
			final MvcResult result = anonymous.perform(post(Routes.PROFILE)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "type": "READER",
							  "name": "Zaida Nova",
							  "description": "Perfil criado no teste de integração.",
							  "birthday": "1990-01-01",
							  "email": "zaida.anon@example.com",
							  "password": "senhaSegura1"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(n.get("type").asText()).isEqualTo("WRITER");
			assertThat(profileRepository.findByEmail("zaida.anon@example.com").orElseThrow().getType())
					.isEqualTo(com.sajitar.backend.domain.model.profile.Profile.Type.WRITER);
		}

		@ParameterizedTest(name = "{0}")
		@ValueSource(strings = { "Basic dXNlcjpwYXNz", "Bearer not-a-jwt" })
		@DisplayName("POST com Authorization inválido ignora type e grava WRITER")
		void postInvalidAuthorizationIgnoresTypeAndDefaultsToWriter(final String authorization) throws Exception {
			final var anonymous = IntegrationAuth.withSecurity(webApplicationContext);
			final var email = "zaida.auth." + authorization.hashCode() + "@example.com";
			final MvcResult result = anonymous.perform(post(Routes.PROFILE)
					.header(HttpHeaders.AUTHORIZATION, authorization)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "type": "READER",
							  "name": "Zaida Nova",
							  "description": "Perfil criado no teste de integração.",
							  "birthday": "1990-01-01",
							  "email": "%s",
							  "password": "senhaSegura1"
							}
							""".formatted(email))
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(n.get("type").asText()).isEqualTo("WRITER");
			assertThat(profileRepository.findByEmail(email).orElseThrow().getType())
					.isEqualTo(com.sajitar.backend.domain.model.profile.Profile.Type.WRITER);
		}

		@Test
		@DisplayName("POST da Carla ignora type e grava WRITER")
		void postCarlaBearerIgnoresTypeAndDefaultsToWriter() throws Exception {
			final var carla = IntegrationAuth.withSecurityAndBearer(webApplicationContext, CARLA_ID);
			final MvcResult result = carla.perform(post(Routes.PROFILE)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "type": "MASTER",
							  "name": "Zaida Nova",
							  "description": "Perfil criado no teste de integração.",
							  "birthday": "1990-01-01",
							  "email": "zaida.carla@example.com",
							  "password": "senhaSegura1"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(n.get("type").asText()).isEqualTo("WRITER");
			assertThat(profileRepository.findByEmail("zaida.carla@example.com").orElseThrow().getType())
					.isEqualTo(com.sajitar.backend.domain.model.profile.Profile.Type.WRITER);
		}

		@Test
		@DisplayName("POST da Alice com type MASTER nasce com twoFactor ligado")
		void postAliceBearerPersistsMasterWithTwoFactorEnabled() throws Exception {
			final MvcResult result = mockMvc.perform(post(Routes.PROFILE)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "type": "MASTER",
							  "name": "Zaida Nova",
							  "description": "Perfil criado no teste de integração.",
							  "birthday": "1990-01-01",
							  "email": "zaida.master@example.com",
							  "password": "senhaSegura1"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(n.get("type").asText()).isEqualTo("MASTER");
			final var persisted = profileRepository.findByEmail("zaida.master@example.com").orElseThrow();
			assertThat(persisted.getType()).isEqualTo(com.sajitar.backend.domain.model.profile.Profile.Type.MASTER);
			assertThat(persisted.isTwoFactor()).isTrue();
		}

		@Test
		@DisplayName("POST com type desconhecido retorna 400")
		void postUnknownTypeReturns400() throws Exception {
			final MvcResult result = mockMvc.perform(post(Routes.PROFILE)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "type": "MEMBER",
							  "name": "Zaida Nova",
							  "description": "Perfil criado no teste de integração.",
							  "birthday": "1990-01-01",
							  "email": "zaida.member@example.com",
							  "password": "senhaSegura1"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "type", "Profile.Type");
		}

		@Test
		@DisplayName("PUT sem type retorna 400")
		void putMissingTypeReturns400() throws Exception {
			final MvcResult result = mockMvc.perform(put(Routes.PROFILE + "/" + ALICE_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name": "Alice Alves",
							  "description": "Uma pessoa criativa e dedicada.",
							  "birthday": "1988-01-10",
							  "twoFactor": false
							}""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "type", "null");
		}

		@Test
		@DisplayName("PUT sem twoFactor retorna 400")
		void putMissingTwoFactorReturns400() throws Exception {
			final MvcResult result = mockMvc.perform(put(Routes.PROFILE + "/" + ALICE_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "type": "MASTER",
							  "name": "Alice Alves",
							  "description": "Uma pessoa criativa e dedicada.",
							  "birthday": "1988-01-10"
							}""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "twoFactor", "must not be null");
		}

		@Test
		@DisplayName("PUT substitui o type vigente")
		void putReplacesType() throws Exception {
			final MvcResult result = mockMvc.perform(put(Routes.PROFILE + "/" + ALICE_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "type": "WRITER",
							  "name": "Alice Alves",
							  "description": "Uma pessoa criativa e dedicada.",
							  "birthday": "1988-01-10",
							  "twoFactor": false
							}""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(n.get("type").asText()).isEqualTo("WRITER");
			assertThat(profileRepository.findById(ALICE_ID).orElseThrow().getType())
					.isEqualTo(com.sajitar.backend.domain.model.profile.Profile.Type.WRITER);
		}

		@Test
		@DisplayName("PATCH type substitui o vigente")
		void patchType() throws Exception {
			final MvcResult result = mockMvc.perform(patch(Routes.PROFILE + "/" + ALICE_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "type": "READER"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(n.get("type").asText()).isEqualTo("READER");
			assertThat(profileRepository.findById(ALICE_ID).orElseThrow().getType())
					.isEqualTo(com.sajitar.backend.domain.model.profile.Profile.Type.READER);
		}

		@Test
		@DisplayName("PATCH type desconhecido retorna 400 {type}")
		void patchUnknownTypeReturns400() throws Exception {
			final var result = mockMvc.perform(patch(Routes.PROFILE + "/" + ALICE_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "type": "MEMBER"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "type", "Profile.Type");
			assertThat(profileRepository.findById(ALICE_ID).orElseThrow().getType())
					.isEqualTo(com.sajitar.backend.domain.model.profile.Profile.Type.MASTER);
		}

		@Test
		@DisplayName("PATCH type nulo mantém o vigente")
		void patchNullTypeKeepsExisting() throws Exception {
			final MvcResult result = mockMvc.perform(patch(Routes.PROFILE + "/" + ALICE_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "type": null
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(n.get("type").asText()).isEqualTo("MASTER");
			assertThat(profileRepository.findById(ALICE_ID).orElseThrow().getType())
					.isEqualTo(com.sajitar.backend.domain.model.profile.Profile.Type.MASTER);
		}

		@Test
		@DisplayName("PATCH twoFactor omitido mantém o vigente")
		void patchOmittedTwoFactorKeepsExisting() throws Exception {
			mockMvc.perform(patch(Routes.PROFILE + "/" + ALICE_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "description": "Uma pessoa criativa e dedicada."
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk());
			assertThat(profileRepository.findById(ALICE_ID).orElseThrow().isTwoFactor()).isTrue();
		}

		@Test
		@DisplayName("PATCH twoFactor nulo retorna 400")
		void patchNullTwoFactorReturns400() throws Exception {
			final MvcResult result = mockMvc.perform(patch(Routes.PROFILE + "/" + ALICE_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "twoFactor": null
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "twoFactor", "must not be null");
		}

		@Test
		@DisplayName("PUT da Carla com type diferente retorna 403 {type}")
		void putCarlaDifferentTypeReturns403() throws Exception {
			final var carla = IntegrationAuth.withSecurityAndBearer(webApplicationContext, CARLA_ID);
			final var result = carla.perform(put(Routes.PROFILE + "/" + CARLA_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "type": "MASTER",
							  "name": "Carla Pereira",
							  "description": "Líder de projeto com foco em inovação.",
							  "birthday": "1975-09-05",
							  "twoFactor": true
							}""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isForbidden())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("type");
			assertThat(n.get("type").get(0).asText()).contains("assigned by a master");
			assertThat(profileRepository.findById(CARLA_ID).orElseThrow().getType())
					.isEqualTo(com.sajitar.backend.domain.model.profile.Profile.Type.READER);
		}

		@Test
		@DisplayName("PUT da Carla com o type vigente retorna 200")
		void putCarlaSameTypeReturns200() throws Exception {
			final var carla = IntegrationAuth.withSecurityAndBearer(webApplicationContext, CARLA_ID);
			final MvcResult result = carla.perform(put(Routes.PROFILE + "/" + CARLA_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "type": "READER",
							  "name": "Carla Atualizada",
							  "description": "Líder de projeto com foco em inovação.",
							  "birthday": "1975-09-05",
							  "twoFactor": false
							}""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(n.get("type").asText()).isEqualTo("READER");
			assertThat(n.get("name").asText()).isEqualTo("Carla Atualizada");
			assertThat(profileRepository.findById(CARLA_ID).orElseThrow().getType())
					.isEqualTo(com.sajitar.backend.domain.model.profile.Profile.Type.READER);
		}

		@Test
		@DisplayName("PATCH da Carla com type diferente retorna 403 {type}")
		void patchCarlaDifferentTypeReturns403() throws Exception {
			final var carla = IntegrationAuth.withSecurityAndBearer(webApplicationContext, CARLA_ID);
			final var result = carla.perform(patch(Routes.PROFILE + "/" + CARLA_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "type": "WRITER"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isForbidden())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("type");
			assertThat(n.get("type").get(0).asText()).contains("assigned by a master");
			assertThat(profileRepository.findById(CARLA_ID).orElseThrow().getType())
					.isEqualTo(com.sajitar.backend.domain.model.profile.Profile.Type.READER);
		}

		@Test
		@DisplayName("PATCH da Carla omitindo type retorna 200")
		void patchCarlaOmittingTypeReturns200() throws Exception {
			final var carla = IntegrationAuth.withSecurityAndBearer(webApplicationContext, CARLA_ID);
			final MvcResult result = carla.perform(patch(Routes.PROFILE + "/" + CARLA_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name": "Carla Pereira"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(n.get("type").asText()).isEqualTo("READER");
			assertThat(profileRepository.findById(CARLA_ID).orElseThrow().getType())
					.isEqualTo(com.sajitar.backend.domain.model.profile.Profile.Type.READER);
		}

		@Test
		@DisplayName("PUT da Alice no nome da Carla retorna 403 {id}")
		void putAliceCannotChangeCarlaName() throws Exception {
			final var result = mockMvc.perform(put(Routes.PROFILE + "/" + CARLA_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "type": "READER",
							  "name": "Carla Alterada",
							  "description": "Líder de projeto com foco em inovação.",
							  "birthday": "1975-09-05",
							  "twoFactor": false
							}""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isForbidden())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("id");
			assertThat(n.get("id").get(0).asText()).contains("authenticated profile");
			assertThat(profileRepository.findById(CARLA_ID).orElseThrow().getName()).isEqualTo("Carla Pereira");
		}

		@Test
		@DisplayName("PUT da Alice no nome e type da Carla retorna 403 {id}")
		void putAliceCannotChangeCarlaNameAndType() throws Exception {
			final var result = mockMvc.perform(put(Routes.PROFILE + "/" + CARLA_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "type": "MASTER",
							  "name": "Carla Alterada",
							  "description": "Líder de projeto com foco em inovação.",
							  "birthday": "1975-09-05",
							  "twoFactor": true
							}""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isForbidden())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("id");
			final var carla = profileRepository.findById(CARLA_ID).orElseThrow();
			assertThat(carla.getName()).isEqualTo("Carla Pereira");
			assertThat(carla.getType()).isEqualTo(com.sajitar.backend.domain.model.profile.Profile.Type.READER);
		}

		@Test
		@DisplayName("PUT da Alice só no type da Carla com os demais iguais retorna 200")
		void putAliceChangesOnlyCarlaType() throws Exception {
			final MvcResult result = mockMvc.perform(put(Routes.PROFILE + "/" + CARLA_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "type": "WRITER",
							  "name": "Carla Pereira",
							  "description": "Líder de projeto com foco em inovação.",
							  "birthday": "1975-09-05",
							  "twoFactor": false
							}""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(n.get("type").asText()).isEqualTo("WRITER");
			assertThat(n.get("name").asText()).isEqualTo("Carla Pereira");
			final var carla = profileRepository.findById(CARLA_ID).orElseThrow();
			assertThat(carla.getType()).isEqualTo(com.sajitar.backend.domain.model.profile.Profile.Type.WRITER);
			assertThat(carla.getName()).isEqualTo("Carla Pereira");
		}

		@Test
		@DisplayName("PUT da Alice liga o próprio twoFactor")
		void putAliceEnablesOwnTwoFactor() throws Exception {
			final MvcResult result = mockMvc.perform(put(Routes.PROFILE + "/" + ALICE_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "type": "MASTER",
							  "name": "Alice Alves",
							  "description": "Uma pessoa criativa e dedicada.",
							  "birthday": "1988-01-10",
							  "twoFactor": true
							}""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactlyInAnyOrder("id", "type", "name", "description");
			assertThat(profileRepository.findById(ALICE_ID).orElseThrow().isTwoFactor()).isTrue();
			final MvcResult details = mockMvc
					.perform(get(Routes.PROFILE + "/" + ALICE_ID + "/details").accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			assertThat(objectMapper.readTree(responseBodyUtf8(details)).get("twoFactor").asBoolean()).isTrue();
		}

		@Test
		@DisplayName("PUT da Alice com twoFactor false retorna 400 {twoFactor}")
		void putAliceTwoFactorFalseReturns400() throws Exception {
			final MvcResult result = mockMvc.perform(put(Routes.PROFILE + "/" + ALICE_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "type": "MASTER",
							  "name": "Alice Alves",
							  "description": "Uma pessoa criativa e dedicada.",
							  "birthday": "1988-01-10",
							  "twoFactor": false
							}""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "twoFactor", "master profile");
			assertThat(profileRepository.findById(ALICE_ID).orElseThrow().isTwoFactor()).isTrue();
		}

		@Test
		@DisplayName("PATCH da Alice com twoFactor false retorna 400 {twoFactor}")
		void patchAliceTwoFactorFalseReturns400() throws Exception {
			final MvcResult result = mockMvc.perform(patch(Routes.PROFILE + "/" + ALICE_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "twoFactor": false
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "twoFactor", "master profile");
			assertThat(profileRepository.findById(ALICE_ID).orElseThrow().isTwoFactor()).isTrue();
		}

		@Test
		@DisplayName("PATCH da Alice promovendo a Carla a MASTER sem twoFactor retorna 400 {twoFactor}")
		void patchAlicePromotingCarlaToMasterWithoutTwoFactorReturns400() throws Exception {
			final MvcResult result = mockMvc.perform(patch(Routes.PROFILE + "/" + CARLA_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "type": "MASTER"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "twoFactor", "master profile");
			assertThat(profileRepository.findById(CARLA_ID).orElseThrow().getType())
					.isEqualTo(com.sajitar.backend.domain.model.profile.Profile.Type.READER);
		}

		@Test
		@DisplayName("PUT da Alice no twoFactor da Carla retorna 403 {id}")
		void putAliceCannotChangeCarlaTwoFactor() throws Exception {
			final var result = mockMvc.perform(put(Routes.PROFILE + "/" + CARLA_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "type": "READER",
							  "name": "Carla Pereira",
							  "description": "Líder de projeto com foco em inovação.",
							  "birthday": "1975-09-05",
							  "twoFactor": true
							}""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isForbidden())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("id");
			assertThat(n.get("id").get(0).asText()).contains("authenticated profile");
			assertThat(profileRepository.findById(CARLA_ID).orElseThrow().isTwoFactor()).isFalse();
		}

		@Test
		@DisplayName("PATCH da Alice no nome da Carla retorna 403 {id}")
		void patchAliceCannotChangeCarlaName() throws Exception {
			final var result = mockMvc.perform(patch(Routes.PROFILE + "/" + CARLA_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name": "Carla Alterada"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isForbidden())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("id");
			assertThat(n.get("id").get(0).asText()).contains("authenticated profile");
			assertThat(profileRepository.findById(CARLA_ID).orElseThrow().getName()).isEqualTo("Carla Pereira");
		}

		@Test
		@DisplayName("PATCH da Alice no nome e type da Carla retorna 403 {id}")
		void patchAliceCannotChangeCarlaNameAndType() throws Exception {
			final var result = mockMvc.perform(patch(Routes.PROFILE + "/" + CARLA_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "type": "MASTER",
							  "name": "Carla Alterada"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isForbidden())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("id");
			final var carla = profileRepository.findById(CARLA_ID).orElseThrow();
			assertThat(carla.getName()).isEqualTo("Carla Pereira");
			assertThat(carla.getType()).isEqualTo(com.sajitar.backend.domain.model.profile.Profile.Type.READER);
		}

		@Test
		@DisplayName("PATCH da Alice só no type da Carla retorna 200")
		void patchAliceChangesOnlyCarlaType() throws Exception {
			final MvcResult result = mockMvc.perform(patch(Routes.PROFILE + "/" + CARLA_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "type": "WRITER"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(n.get("type").asText()).isEqualTo("WRITER");
			assertThat(n.get("name").asText()).isEqualTo("Carla Pereira");
			assertThat(profileRepository.findById(CARLA_ID).orElseThrow().getType())
					.isEqualTo(com.sajitar.backend.domain.model.profile.Profile.Type.WRITER);
		}

		@Test
		@DisplayName("PATCH da Alice no twoFactor da Carla retorna 403 {id}")
		void patchAliceCannotChangeCarlaTwoFactor() throws Exception {
			final var result = mockMvc.perform(patch(Routes.PROFILE + "/" + CARLA_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "twoFactor": true
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isForbidden())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("id");
			assertThat(profileRepository.findById(CARLA_ID).orElseThrow().isTwoFactor()).isFalse();
		}

		@Test
		@DisplayName("PATCH da Carla liga twoFactor e desligar apaga o SIGN_IN")
		void patchCarlaTwoFactorDeletesSignInWhenDisabled() throws Exception {
			final var carla = IntegrationAuth.withSecurityAndBearer(webApplicationContext, CARLA_ID);
			carla.perform(patch(Routes.PROFILE + "/" + CARLA_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "twoFactor": true
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk());
			assertThat(profileRepository.findById(CARLA_ID).orElseThrow().isTwoFactor()).isTrue();
			final var checker = Checker.create(CARLA_ID, Checker.Type.SIGN_IN);
			checkerRepository.save(CheckerJpaEntity.builder()
					.id(checker.id())
					.profileId(checker.profileId())
					.type(checker.type())
					.code(checker.code())
					.payload(checker.payload())
					.build());
			checkerRepository.flush();

			carla.perform(patch(Routes.PROFILE + "/" + CARLA_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "twoFactor": false
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk());

			assertThat(profileRepository.findById(CARLA_ID).orElseThrow().isTwoFactor()).isFalse();
			assertThat(checkerRepository.findByProfileIdAndType(CARLA_ID, Checker.Type.SIGN_IN)).isEmpty();
		}

		@Test
		@DisplayName("PATCH da Carla no nome da Alice retorna 403 {id}")
		void patchCarlaCannotChangeAliceName() throws Exception {
			final var carla = IntegrationAuth.withSecurityAndBearer(webApplicationContext, CARLA_ID);
			final var result = carla.perform(patch(Routes.PROFILE + "/" + ALICE_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "name": "Alice Alterada"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isForbidden())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("id");
			assertThat(n.get("id").get(0).asText()).contains("authenticated profile");
			assertThat(profileRepository.findById(ALICE_ID).orElseThrow().getName()).isEqualTo(ALICE_NAME);
		}

		@Test
		@DisplayName("DELETE do próprio perfil com VERIFY_EMAIL retorna 403 {email}")
		void deleteOwnUnverifiedReturns403() throws Exception {
			final var result = mockMvc.perform(delete(Routes.PROFILE + "/" + ALICE_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "code": "123456"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isForbidden())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("email");
			assertThat(profileRepository.findById(ALICE_ID)).isPresent();
		}

		@Test
		@DisplayName("DELETE com id diferente da sessão retorna 403 {id}")
		void deleteUnknownIdReturns403() throws Exception {
			final var result = mockMvc.perform(delete(Routes.PROFILE + "/" + UNKNOWN_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "code": "123456"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isForbidden())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("id");
			assertThat(n.get("id").get(0).asText()).contains("authenticated profile");
		}

		@Test
		@DisplayName("DELETE de perfil alheio por MASTER retorna 403 {id}")
		void masterCannotDeleteAnotherProfile() throws Exception {
			final var result = mockMvc.perform(delete(Routes.PROFILE + "/" + CARLA_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "code": "123456"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isForbidden())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("id");
			assertThat(profileRepository.findById(CARLA_ID)).isPresent();
		}

		@Test
		@DisplayName("PUT com password extra preserva as sessões do perfil")
		void putWithUnknownPasswordKeepsSessions() throws Exception {
			mockMvc.perform(put(Routes.PROFILE + "/" + ALICE_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "type": "MASTER",
							  "name": "Alice Alves",
							  "description": "Uma pessoa criativa e dedicada.",
							  "birthday": "1988-01-10",
							  "email": "alice@example.com",
							  "password": "novaSenhaSegura1",
							  "twoFactor": true
							}""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk());

			mockMvc.perform(get(Routes.PROFILE + "/" + ALICE_ID).accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk());
		}

		@Test
		@DisplayName("PUT sem senha preserva as sessões do perfil")
		void putWithoutPasswordKeepsSessions() throws Exception {
			mockMvc.perform(put(Routes.PROFILE + "/" + ALICE_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "type": "MASTER",
							  "name": "Alice Alves",
							  "description": "Uma pessoa criativa e dedicada.",
							  "birthday": "1988-01-10",
							  "email": "alice@example.com",
							  "twoFactor": true
							}""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk());

			mockMvc.perform(get(Routes.PROFILE + "/" + ALICE_ID).accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk());
		}

		@Test
		@DisplayName("PATCH com password extra não encerra as sessões")
		void patchWithUnknownPasswordKeepsSessions() throws Exception {
			mockMvc.perform(patch(Routes.PROFILE + "/" + ALICE_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "password": "novaSenhaSegura1"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk());

			mockMvc.perform(get(Routes.PROFILE + "/" + ALICE_ID).accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk());
			final var persisted = profileRepository.findById(ALICE_ID).orElseThrow();
			assertThat(persisted.getPassword()).isEqualTo(PASSWORD_HASH);
		}

		@Test
		@DisplayName("POST /profiles/password troca a senha, apaga CHANGE_PASSWORD e mantém sessões")
		void postPasswordRehashesAndDeletesCheckerWithoutSignoutAllSessions() throws Exception {
			assertThat(checkerRepository.findByProfileIdAndType(ALICE_ID, Checker.Type.CHANGE_PASSWORD)).isPresent();
			final var result = mockMvc.perform(post(Routes.PROFILE + "/password")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "currentPassword": "senhaSegura1",
							  "newPassword": "novaSenhaSegura1"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNoContent())
					.andReturn();
			assertNoContentBody(result);
			final var persisted = profileRepository.findById(ALICE_ID).orElseThrow();
			assertThat(persisted.getPassword()).startsWith("$2a$");
			assertThat(persisted.getPassword()).isNotEqualTo(PASSWORD_HASH);
			assertThat(persisted.getPassword()).hasSize(60);
			assertThat(checkerRepository.findByProfileIdAndType(ALICE_ID, Checker.Type.CHANGE_PASSWORD)).isEmpty();
			mockMvc.perform(get(Routes.PROFILE + "/" + ALICE_ID).accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk());
		}

		@Test
		@DisplayName("POST /profiles/password com signoutAllSessions true encerra as sessões")
		void postPasswordWithSignoutAllSessionsEndsSessions() throws Exception {
			final var result = mockMvc.perform(post(Routes.PROFILE + "/password")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "currentPassword": "senhaSegura1",
							  "newPassword": "novaSenhaSegura1",
							  "signoutAllSessions": true
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNoContent())
					.andReturn();
			assertNoContentBody(result);
			final var persisted = profileRepository.findById(ALICE_ID).orElseThrow();
			assertThat(persisted.getPassword()).startsWith("$2a$");
			assertThat(persisted.getPassword()).isNotEqualTo(PASSWORD_HASH);
			mockMvc.perform(get(Routes.PROFILE + "/" + ALICE_ID).accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isUnauthorized());
		}

		@Test
		@DisplayName("POST /profiles/password com senhas iguais retorna 400 em newPassword")
		void postPasswordRejectsEqualPasswords() throws Exception {
			final var result = mockMvc.perform(post(Routes.PROFILE + "/password")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "currentPassword": "senhaSegura1",
							  "newPassword": "senhaSegura1"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "newPassword", "differ");
			final var persisted = profileRepository.findById(ALICE_ID).orElseThrow();
			assertThat(persisted.getPassword()).isEqualTo(PASSWORD_HASH);
		}

		@Test
		@DisplayName("POST /profiles/password com senha nova curta retorna 400")
		void postPasswordRejectsShortNewPassword() throws Exception {
			final var result = mockMvc.perform(post(Routes.PROFILE + "/password")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "currentPassword": "senhaSegura1",
							  "newPassword": "1234567"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "newPassword", "between");
		}

		@Test
		@DisplayName("POST /profiles/password com senha atual errada retorna 401")
		void postPasswordRejectsWrongCurrentPassword() throws Exception {
			final var result = mockMvc.perform(post(Routes.PROFILE + "/password")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "currentPassword": "senhaErrada1",
							  "newPassword": "novaSenhaSegura1"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isUnauthorized())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("credentials");
			assertThat(n.get("credentials").get(0).asText()).contains("valid credentials");
			final var persisted = profileRepository.findById(ALICE_ID).orElseThrow();
			assertThat(persisted.getPassword()).isEqualTo(PASSWORD_HASH);
		}

		@Test
		@DisplayName("POST /profiles/password sem Bearer retorna 401 {token}")
		void postPasswordWithoutBearerReturns401() throws Exception {
			final var anonymous = IntegrationAuth.withSecurity(webApplicationContext);
			final MvcResult result = anonymous.perform(post(Routes.PROFILE + "/password")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "currentPassword": "senhaSegura1",
							  "newPassword": "novaSenhaSegura1"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isUnauthorized())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("token");
			assertThat(n.get("token").get(0).asText()).contains("bearer token");
		}
	}

	@Nested
	@Transactional
	@DisplayName("POST /profiles/email/recovery, /confirm e /change")
	class ChangeEmail {

		private MockMvc carlaMvc;

		@BeforeEach
		void setUpCarla() {
			SessionSettlementFixture.clear(redis);
			carlaMvc = IntegrationAuth.withSecurityAndBearer(webApplicationContext, CARLA_ID);
		}

		@Test
		@DisplayName("401 sem Bearer")
		void recoveryWithoutBearerReturns401() throws Exception {
			final var publicMvc = IntegrationAuth.withSecurity(webApplicationContext);
			final var result = publicMvc.perform(post(Routes.PROFILE + "/email/recovery")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isUnauthorized())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("token");
		}

		@Test
		@DisplayName("403 quando o perfil tem VERIFY_EMAIL")
		void recoveryRejectsUnverifiedEmail() throws Exception {
			final var aliceMvc = IntegrationAuth.withSecurityAndAliceBearer(webApplicationContext);
			final var result = aliceMvc.perform(post(Routes.PROFILE + "/email/recovery")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isForbidden())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("email");
			assertThat(n.get("email").get(0).asText()).contains("verified email");
			if (recordingMailer != null) {
				assertThat(recordingMailer.sent()).isEmpty();
			}
		}

		@Test
		@DisplayName("403 no confirm quando o perfil tem VERIFY_EMAIL")
		void confirmRejectsUnverifiedEmail() throws Exception {
			final var aliceMvc = IntegrationAuth.withSecurityAndAliceBearer(webApplicationContext);
			final var result = aliceMvc.perform(post(Routes.PROFILE + "/email/confirm")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "code": "123456",
							  "newEmail": "alice.nova@example.com"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isForbidden())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("email");
		}

		@Test
		@DisplayName("403 na troca quando o perfil tem VERIFY_EMAIL")
		void changeRejectsUnverifiedEmail() throws Exception {
			final var aliceMvc = IntegrationAuth.withSecurityAndAliceBearer(webApplicationContext);
			final var result = aliceMvc.perform(post(Routes.PROFILE + "/email/change")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "code": "123456"
							}
							""")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isForbidden())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("email");
		}

		@Test
		@DisplayName("204 cria CHANGE_EMAIL e envia o código ao e-mail vigente")
		void recoveryCreatesCheckerAndSendsMail() throws Exception {
			final var result = recover()
					.andExpect(status().isNoContent())
					.andReturn();
			assertNoContentBody(result);
			final var checker = checkerRepository
					.findByProfileIdAndType(CARLA_ID, Checker.Type.CHANGE_EMAIL)
					.orElseThrow();
			assertThat(checker.getCode()).matches("^[0-9]{6}$");
			assertThat(checker.getPayload()).isNull();
			if (recordingMailer != null) {
				assertThat(recordingMailer.sent()).hasSize(1);
				final var mail = recordingMailer.sent().getFirst();
				assertThat(mail.to()).isEqualTo("carla@example.com");
				assertThat(mail.subject()).doesNotContain(checker.getCode());
				assertThat(mail.body()).contains(checker.getCode());
			}
		}

		@Test
		@DisplayName("204 gira o código com payload nulo e invalida o anterior")
		void recoveryRotatesCodeAndInvalidatesPrevious() throws Exception {
			recover().andExpect(status().isNoContent());
			final var first = checkerRepository
					.findByProfileIdAndType(CARLA_ID, Checker.Type.CHANGE_EMAIL)
					.orElseThrow();
			final var previousCode = first.getCode();
			if (recordingMailer != null) {
				recordingMailer.clear();
			}

			recover().andExpect(status().isNoContent());

			final var second = checkerRepository
					.findByProfileIdAndType(CARLA_ID, Checker.Type.CHANGE_EMAIL)
					.orElseThrow();
			assertThat(second.getId()).isEqualTo(first.getId());
			assertThat(second.getCode()).isNotEqualTo(previousCode);
			assertThat(second.getPayload()).isNull();
			confirm(previousCode, "carla.nova@example.com").andExpect(status().isUnauthorized());
			confirm(second.getCode(), "carla.nova@example.com").andExpect(status().isNoContent());
		}

		@Test
		@DisplayName("401 quando o CHANGE_EMAIL tem mais de 30 minutos")
		void recoveryRejectsExpiredChecker() throws Exception {
			persistExpiredChangeEmail(CARLA_ID, "123456", null);

			final var result = recover()
					.andExpect(status().isUnauthorized())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("code");
			if (recordingMailer != null) {
				assertThat(recordingMailer.sent()).isEmpty();
			}
			final var checker = checkerRepository
					.findByProfileIdAndType(CARLA_ID, Checker.Type.CHANGE_EMAIL)
					.orElseThrow();
			assertThat(checker.getCode()).isEqualTo("123456");
		}

		@Test
		@DisplayName("204 confirma o código vigente, grava o payload e envia ao e-mail novo")
		void confirmStoresPayloadAndMailsNewAddress() throws Exception {
			recover().andExpect(status().isNoContent());
			final var code = checkerRepository
					.findByProfileIdAndType(CARLA_ID, Checker.Type.CHANGE_EMAIL)
					.orElseThrow()
					.getCode();
			if (recordingMailer != null) {
				recordingMailer.clear();
			}

			final var result = confirm(code, "carla.nova@example.com")
					.andExpect(status().isNoContent())
					.andReturn();
			assertNoContentBody(result);
			final var checker = checkerRepository
					.findByProfileIdAndType(CARLA_ID, Checker.Type.CHANGE_EMAIL)
					.orElseThrow();
			assertThat(checker.getPayload()).isEqualTo("carla.nova@example.com");
			assertThat(checker.getCode()).isNotEqualTo(code);
			assertThat(profileRepository.findById(CARLA_ID).orElseThrow().getEmail()).isEqualTo("carla@example.com");
			if (recordingMailer != null) {
				assertThat(recordingMailer.sent()).hasSize(1);
				final var mail = recordingMailer.sent().getFirst();
				assertThat(mail.to()).isEqualTo("carla.nova@example.com");
				assertThat(mail.body()).contains(checker.getCode());
			}
		}

		@Test
		@DisplayName("204 com payload gravado recomeça e aceita outro newEmail no confirm")
		void recoveryWithPayloadRestartsAndAllowsNewConfirmEmail() throws Exception {
			recover().andExpect(status().isNoContent());
			final var firstCode = checkerRepository
					.findByProfileIdAndType(CARLA_ID, Checker.Type.CHANGE_EMAIL)
					.orElseThrow()
					.getCode();
			confirm(firstCode, "carla.nova@example.com").andExpect(status().isNoContent());
			final var before = checkerRepository
					.findByProfileIdAndType(CARLA_ID, Checker.Type.CHANGE_EMAIL)
					.orElseThrow();
			final var previousCode = before.getCode();
			if (recordingMailer != null) {
				recordingMailer.clear();
			}

			recover().andExpect(status().isNoContent());

			final var after = checkerRepository
					.findByProfileIdAndType(CARLA_ID, Checker.Type.CHANGE_EMAIL)
					.orElseThrow();
			assertThat(after.getId()).isEqualTo(before.getId());
			assertThat(after.getPayload()).isNull();
			assertThat(after.getCode()).isNotEqualTo(previousCode);
			assertThat(profileRepository.findById(CARLA_ID).orElseThrow().getEmail()).isEqualTo("carla@example.com");
			if (recordingMailer != null) {
				assertThat(recordingMailer.sent()).hasSize(1);
				final var mail = recordingMailer.sent().getFirst();
				assertThat(mail.to()).isEqualTo("carla@example.com");
				assertThat(mail.body()).contains(after.getCode());
			}
			confirm(after.getCode(), "carla.outra@example.com").andExpect(status().isNoContent());
			assertThat(checkerRepository.findByProfileIdAndType(CARLA_ID, Checker.Type.CHANGE_EMAIL)
					.orElseThrow()
					.getPayload()).isEqualTo("carla.outra@example.com");
		}

		@Test
		@DisplayName("400 quando o e-mail novo é o vigente")
		void confirmRejectsSameEmail() throws Exception {
			recover().andExpect(status().isNoContent());
			final var code = checkerRepository
					.findByProfileIdAndType(CARLA_ID, Checker.Type.CHANGE_EMAIL)
					.orElseThrow()
					.getCode();

			final var result = confirm(code, "carla@example.com")
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "newEmail", "differ");
		}

		@Test
		@DisplayName("401 quando o código diverge e o vigente não muda")
		void confirmRejectsWrongCodeWithoutRotating() throws Exception {
			recover().andExpect(status().isNoContent());
			final var before = checkerRepository
					.findByProfileIdAndType(CARLA_ID, Checker.Type.CHANGE_EMAIL)
					.orElseThrow();

			final var result = confirm("000000", "carla.nova@example.com")
					.andExpect(status().isUnauthorized())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("code");
			final var after = checkerRepository
					.findByProfileIdAndType(CARLA_ID, Checker.Type.CHANGE_EMAIL)
					.orElseThrow();
			assertThat(after.getCode()).isEqualTo(before.getCode());
			assertThat(after.getPayload()).isNull();
		}

		@Test
		@DisplayName("401 quando o payload já está preenchido")
		void confirmRejectsWhenPayloadAlreadyFilled() throws Exception {
			recover().andExpect(status().isNoContent());
			final var firstCode = checkerRepository
					.findByProfileIdAndType(CARLA_ID, Checker.Type.CHANGE_EMAIL)
					.orElseThrow()
					.getCode();
			confirm(firstCode, "carla.nova@example.com").andExpect(status().isNoContent());
			final var before = checkerRepository
					.findByProfileIdAndType(CARLA_ID, Checker.Type.CHANGE_EMAIL)
					.orElseThrow();

			final var result = confirm(before.getCode(), "carla.outra@example.com")
					.andExpect(status().isUnauthorized())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("code");
			final var after = checkerRepository
					.findByProfileIdAndType(CARLA_ID, Checker.Type.CHANGE_EMAIL)
					.orElseThrow();
			assertThat(after.getCode()).isEqualTo(before.getCode());
			assertThat(after.getPayload()).isEqualTo("carla.nova@example.com");
		}

		@Test
		@DisplayName("409 quando o e-mail novo pertence a outro perfil")
		void confirmRejectsTakenEmailWithoutConsuming() throws Exception {
			recover().andExpect(status().isNoContent());
			final var before = checkerRepository
					.findByProfileIdAndType(CARLA_ID, Checker.Type.CHANGE_EMAIL)
					.orElseThrow();

			final var result = confirm(before.getCode(), "bruno@example.com")
					.andExpect(status().isConflict())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("email");
			final var after = checkerRepository
					.findByProfileIdAndType(CARLA_ID, Checker.Type.CHANGE_EMAIL)
					.orElseThrow();
			assertThat(after.getCode()).isEqualTo(before.getCode());
			assertThat(after.getPayload()).isNull();
		}

		@Test
		@DisplayName("400 quando o código está mal formado")
		void confirmRejectsMalformedCode() throws Exception {
			final var result = confirm("12a456", "carla.nova@example.com")
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "code", "6 digits");
		}

		@Test
		@DisplayName("400 quando o e-mail novo está mal formado")
		void confirmRejectsMalformedEmail() throws Exception {
			final var result = confirm("123456", "nao-e-email")
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "newEmail", "well-formed email");
		}

		@Test
		@DisplayName("204 troca o e-mail, encerra as sessões e apaga o checker")
		void changeUpdatesEmailWipesAndDeletesChecker() throws Exception {
			final var session = IntegrationAuth.openSession(webApplicationContext, CARLA_ID, false);
			recover().andExpect(status().isNoContent());
			final var firstCode = checkerRepository
					.findByProfileIdAndType(CARLA_ID, Checker.Type.CHANGE_EMAIL)
					.orElseThrow()
					.getCode();
			confirm(firstCode, "carla.nova@example.com").andExpect(status().isNoContent());
			final var secondCode = checkerRepository
					.findByProfileIdAndType(CARLA_ID, Checker.Type.CHANGE_EMAIL)
					.orElseThrow()
					.getCode();

			final var result = change(secondCode)
					.andExpect(status().isNoContent())
					.andReturn();
			assertNoContentBody(result);
			assertThat(profileRepository.findById(CARLA_ID).orElseThrow().getEmail())
					.isEqualTo("carla.nova@example.com");
			assertThat(checkerRepository.findByProfileIdAndType(CARLA_ID, Checker.Type.CHANGE_EMAIL)).isEmpty();
			IntegrationAuth.withSecurity(webApplicationContext)
					.perform(get(Routes.PROFILE + "/" + CARLA_ID)
							.header("Authorization", "Bearer " + session.access().value())
							.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isUnauthorized());
		}

		@Test
		@DisplayName("401 quando o código da primeira etapa tenta concluir a troca")
		void changeRejectsFirstStageCode() throws Exception {
			recover().andExpect(status().isNoContent());
			final var firstCode = checkerRepository
					.findByProfileIdAndType(CARLA_ID, Checker.Type.CHANGE_EMAIL)
					.orElseThrow()
					.getCode();

			final var result = change(firstCode)
					.andExpect(status().isUnauthorized())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("code");
			assertThat(profileRepository.findById(CARLA_ID).orElseThrow().getEmail()).isEqualTo("carla@example.com");
		}

		@Test
		@DisplayName("409 na troca quando o payload já foi registrado por outro perfil")
		void changeRejectsTakenPayloadWithoutWipe() throws Exception {
			recover().andExpect(status().isNoContent());
			final var firstCode = checkerRepository
					.findByProfileIdAndType(CARLA_ID, Checker.Type.CHANGE_EMAIL)
					.orElseThrow()
					.getCode();
			confirm(firstCode, "carla.livre@example.com").andExpect(status().isNoContent());
			final var checker = checkerRepository
					.findByProfileIdAndType(CARLA_ID, Checker.Type.CHANGE_EMAIL)
					.orElseThrow();
			final var alice = profileRepository.findById(ALICE_ID).orElseThrow();
			alice.setEmail("carla.livre@example.com");
			profileRepository.save(alice);
			profileRepository.flush();

			final var result = change(checker.getCode())
					.andExpect(status().isConflict())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("email");
			assertThat(checkerRepository.findByProfileIdAndType(CARLA_ID, Checker.Type.CHANGE_EMAIL)).isPresent();
			assertThat(profileRepository.findById(CARLA_ID).orElseThrow().getEmail()).isEqualTo("carla@example.com");
		}

		@Test
		@DisplayName("400 quando o código da troca está mal formado")
		void changeRejectsMalformedCode() throws Exception {
			final var result = change("12a456")
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "code", "6 digits");
		}

		@Test
		@DisplayName("429 no pedido depois do teto, com Retry-After")
		void recoveryReturns429WhenLimitIsExceeded() throws Exception {
			MvcResult last = null;
			for (int i = 0; i < 6; i++) {
				last = recover().andReturn();
			}
			assertThat(last.getResponse().getStatus()).isEqualTo(429);
			assertThat(Integer.parseInt(last.getResponse().getHeader(HttpHeaders.RETRY_AFTER))).isPositive();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(last));
			assertThat(jsonObjectKeys(n)).containsExactly("credentials");
			assertThat(n.get("credentials").get(0).asText()).contains("wait");
		}

		private org.springframework.test.web.servlet.ResultActions recover() throws Exception {
			return carlaMvc.perform(post(Routes.PROFILE + "/email/recovery")
					.accept(MediaType.APPLICATION_JSON));
		}

		private org.springframework.test.web.servlet.ResultActions confirm(final String code, final String newEmail)
				throws Exception {
			return carlaMvc.perform(post(Routes.PROFILE + "/email/confirm")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "code": "%s",
							  "newEmail": "%s"
							}
							""".formatted(code, newEmail))
					.accept(MediaType.APPLICATION_JSON));
		}

		private org.springframework.test.web.servlet.ResultActions change(final String code) throws Exception {
			return carlaMvc.perform(post(Routes.PROFILE + "/email/change")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "code": "%s"
							}
							""".formatted(code))
					.accept(MediaType.APPLICATION_JSON));
		}

		private void persistExpiredChangeEmail(final UUID profileId, final String code, final String payload) {
			checkerRepository.save(CheckerJpaEntity.builder()
					.id(Checker.uuidV7At(Instant.now().minus(Duration.ofMinutes(31))))
					.profileId(profileId)
					.type(Checker.Type.CHANGE_EMAIL)
					.code(code)
					.payload(payload)
					.build());
			checkerRepository.flush();
		}
	}

	@Nested
	@Transactional
	@DisplayName("POST /profiles/deletion e DELETE /profiles/{id}")
	class DeleteOwnProfile {

		private MockMvc carlaMvc;

		@BeforeEach
		void setUpCarla() {
			SessionSettlementFixture.clear(redis);
			carlaMvc = IntegrationAuth.withSecurityAndBearer(webApplicationContext, CARLA_ID);
		}

		@Test
		@DisplayName("401 sem Bearer no pedido")
		void requestWithoutBearerReturns401() throws Exception {
			final var publicMvc = IntegrationAuth.withSecurity(webApplicationContext);
			final var result = publicMvc.perform(post(Routes.PROFILE + "/deletion")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isUnauthorized())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("token");
		}

		@Test
		@DisplayName("403 no pedido quando o perfil tem VERIFY_EMAIL")
		void requestRejectsUnverifiedEmail() throws Exception {
			final var aliceMvc = IntegrationAuth.withSecurityAndAliceBearer(webApplicationContext);
			final var result = aliceMvc.perform(post(Routes.PROFILE + "/deletion")
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isForbidden())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("email");
			assertThat(n.get("email").get(0).asText()).contains("verified email");
			if (recordingMailer != null) {
				assertThat(recordingMailer.sent()).isEmpty();
			}
		}

		@Test
		@DisplayName("204 cria DELETE_PROFILE e envia o código ao e-mail vigente")
		void requestCreatesCheckerAndSendsMail() throws Exception {
			final var result = requestDeletion()
					.andExpect(status().isNoContent())
					.andReturn();
			assertNoContentBody(result);
			final var checker = checkerRepository
					.findByProfileIdAndType(CARLA_ID, Checker.Type.DELETE_PROFILE)
					.orElseThrow();
			assertThat(checker.getCode()).matches("^[0-9]{6}$");
			assertThat(checker.getPayload()).isNull();
			if (recordingMailer != null) {
				assertThat(recordingMailer.sent()).hasSize(1);
				final var mail = recordingMailer.sent().getFirst();
				assertThat(mail.to()).isEqualTo("carla@example.com");
				assertThat(mail.subject()).doesNotContain(checker.getCode());
				assertThat(mail.body()).contains(checker.getCode());
			}
		}

		@Test
		@DisplayName("204 gira o código e invalida o anterior")
		void requestRotatesCodeAndInvalidatesPrevious() throws Exception {
			requestDeletion().andExpect(status().isNoContent());
			final var first = checkerRepository
					.findByProfileIdAndType(CARLA_ID, Checker.Type.DELETE_PROFILE)
					.orElseThrow();
			final var previousCode = first.getCode();
			if (recordingMailer != null) {
				recordingMailer.clear();
			}

			requestDeletion().andExpect(status().isNoContent());

			final var second = checkerRepository
					.findByProfileIdAndType(CARLA_ID, Checker.Type.DELETE_PROFILE)
					.orElseThrow();
			assertThat(second.getId()).isEqualTo(first.getId());
			assertThat(second.getCode()).isNotEqualTo(previousCode);
			assertThat(second.getPayload()).isNull();
			confirmDeletion(previousCode).andExpect(status().isUnauthorized());
			confirmDeletion(second.getCode()).andExpect(status().isNoContent());
			assertThat(profileRepository.findById(CARLA_ID)).isEmpty();
		}

		@Test
		@DisplayName("401 quando o DELETE_PROFILE tem mais de 30 minutos")
		void requestRejectsExpiredChecker() throws Exception {
			persistExpiredDeleteProfile(CARLA_ID, "123456");

			final var result = requestDeletion()
					.andExpect(status().isUnauthorized())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("code");
			if (recordingMailer != null) {
				assertThat(recordingMailer.sent()).isEmpty();
			}
			final var checker = checkerRepository
					.findByProfileIdAndType(CARLA_ID, Checker.Type.DELETE_PROFILE)
					.orElseThrow();
			assertThat(checker.getCode()).isEqualTo("123456");
		}

		@Test
		@DisplayName("204 exclui o perfil, o checker no CASCADE e encerra as sessões")
		void deleteRemovesProfileCheckerAndWipesSessions() throws Exception {
			final var session = IntegrationAuth.openSession(webApplicationContext, CARLA_ID, false);
			requestDeletion().andExpect(status().isNoContent());
			final var code = checkerRepository
					.findByProfileIdAndType(CARLA_ID, Checker.Type.DELETE_PROFILE)
					.orElseThrow()
					.getCode();

			final var result = confirmDeletion(code)
					.andExpect(status().isNoContent())
					.andReturn();
			assertNoContentBody(result);
			entityManager.flush();
			entityManager.clear();
			assertThat(profileRepository.findById(CARLA_ID)).isEmpty();
			assertThat(checkerRepository.findByProfileIdAndType(CARLA_ID, Checker.Type.DELETE_PROFILE)).isEmpty();
			IntegrationAuth.withSecurity(webApplicationContext)
					.perform(get(Routes.PROFILE + "/" + CARLA_ID)
							.header("Authorization", "Bearer " + session.access().value())
							.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isUnauthorized());
		}

		@Test
		@DisplayName("404 quando o id da sessão já não existe")
		void deleteOwnMissingIdReturns404() throws Exception {
			requestDeletion().andExpect(status().isNoContent());
			final var code = checkerRepository
					.findByProfileIdAndType(CARLA_ID, Checker.Type.DELETE_PROFILE)
					.orElseThrow()
					.getCode();
			profileRepository.deleteById(CARLA_ID);
			profileRepository.flush();

			final var result = confirmDeletion(code)
					.andExpect(status().isNotFound())
					.andReturn();
			assertNoContentBody(result);
		}

		@Test
		@DisplayName("400 quando o código é mal formado")
		void deleteMalformedCodeReturns400() throws Exception {
			final var result = confirmDeletion("12a456")
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "code", "6 digits");
		}

		@Test
		@DisplayName("401 quando o código não confere e o vigente não muda")
		void deleteMismatchedCodeDoesNotRotate() throws Exception {
			requestDeletion().andExpect(status().isNoContent());
			final var before = checkerRepository
					.findByProfileIdAndType(CARLA_ID, Checker.Type.DELETE_PROFILE)
					.orElseThrow();

			final var result = confirmDeletion("123456")
					.andExpect(status().isUnauthorized())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("code");
			final var after = checkerRepository
					.findByProfileIdAndType(CARLA_ID, Checker.Type.DELETE_PROFILE)
					.orElseThrow();
			assertThat(after.getCode()).isEqualTo(before.getCode());
			assertThat(profileRepository.findById(CARLA_ID)).isPresent();
		}

		@Test
		@DisplayName("401 quando o checker de exclusão está vencido")
		void deleteExpiredCheckerReturns401() throws Exception {
			persistExpiredDeleteProfile(CARLA_ID, "654321");

			final var result = confirmDeletion("654321")
					.andExpect(status().isUnauthorized())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("code");
			assertThat(profileRepository.findById(CARLA_ID)).isPresent();
		}

		@Test
		@DisplayName("429 no pedido depois do teto, com Retry-After")
		void requestReturns429WhenLimitIsExceeded() throws Exception {
			MvcResult last = null;
			for (int i = 0; i < 6; i++) {
				last = requestDeletion().andReturn();
			}
			assertThat(last.getResponse().getStatus()).isEqualTo(429);
			assertThat(Integer.parseInt(last.getResponse().getHeader(HttpHeaders.RETRY_AFTER))).isPositive();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(last));
			assertThat(jsonObjectKeys(n)).containsExactly("credentials");
			assertThat(n.get("credentials").get(0).asText()).contains("wait");
		}

		private org.springframework.test.web.servlet.ResultActions requestDeletion() throws Exception {
			return carlaMvc.perform(post(Routes.PROFILE + "/deletion")
					.accept(MediaType.APPLICATION_JSON));
		}

		private org.springframework.test.web.servlet.ResultActions confirmDeletion(final String code) throws Exception {
			return carlaMvc.perform(delete(Routes.PROFILE + "/" + CARLA_ID)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "code": "%s"
							}
							""".formatted(code))
					.accept(MediaType.APPLICATION_JSON));
		}

		private void persistExpiredDeleteProfile(final UUID profileId, final String code) {
			checkerRepository.save(CheckerJpaEntity.builder()
					.id(Checker.uuidV7At(Instant.now().minus(Duration.ofMinutes(31))))
					.profileId(profileId)
					.type(Checker.Type.DELETE_PROFILE)
					.code(code)
					.build());
			checkerRepository.flush();
		}
	}

	@Nested
	@Transactional
	@DisplayName("POST /profiles/password/recovery e /confirm")
	class PasswordRecovery {

		private MockMvc publicMvc;

		@BeforeEach
		void setUpPublic() {
			SessionSettlementFixture.clear(redis);
			publicMvc = IntegrationAuth.withSecurity(webApplicationContext);
		}

		@Test
		@DisplayName("204 cria CHANGE_PASSWORD e envia o e-mail")
		void recoveryCreatesCheckerAndSendsMail() throws Exception {
			final var result = recover("bruno@example.com")
					.andExpect(status().isNoContent())
					.andReturn();
			assertNoContentBody(result);
			final var checker = checkerRepository
					.findByProfileIdAndType(BRUNO_ID, Checker.Type.CHANGE_PASSWORD)
					.orElseThrow();
			assertThat(checker.getCode()).matches("^[0-9]{6}$");
			if (recordingMailer != null) {
				assertThat(recordingMailer.sent()).hasSize(1);
				final var mail = recordingMailer.sent().getFirst();
				assertThat(mail.to()).isEqualTo("bruno@example.com");
				assertThat(mail.subject()).doesNotContain(checker.getCode());
				assertThat(mail.body()).contains(checker.getCode());
			}
		}

		@Test
		@DisplayName("204 gira o código e invalida o anterior")
		void recoveryRotatesCodeAndInvalidatesPrevious() throws Exception {
			recover("bruno@example.com").andExpect(status().isNoContent());
			final var first = checkerRepository
					.findByProfileIdAndType(BRUNO_ID, Checker.Type.CHANGE_PASSWORD)
					.orElseThrow();
			final var previousCode = first.getCode();
			if (recordingMailer != null) {
				recordingMailer.clear();
			}

			recover("bruno@example.com").andExpect(status().isNoContent());

			final var second = checkerRepository
					.findByProfileIdAndType(BRUNO_ID, Checker.Type.CHANGE_PASSWORD)
					.orElseThrow();
			assertThat(second.getId()).isEqualTo(first.getId());
			assertThat(second.getCode()).isNotEqualTo(previousCode);
			confirm("bruno@example.com", previousCode, "novaSenhaSegura1")
					.andExpect(status().isUnauthorized());
			confirm("bruno@example.com", second.getCode(), "novaSenhaSegura1")
					.andExpect(status().isNoContent());
		}

		@Test
		@DisplayName("204 sem e-mail quando o perfil tem VERIFY_EMAIL")
		void recoveryIsSilentWhenEmailIsUnverified() throws Exception {
			final var result = recover(ALICE_EMAIL)
					.andExpect(status().isNoContent())
					.andReturn();
			assertNoContentBody(result);
			if (recordingMailer != null) {
				assertThat(recordingMailer.sent()).isEmpty();
			}
			final var checker = checkerRepository
					.findByProfileIdAndType(ALICE_ID, Checker.Type.CHANGE_PASSWORD)
					.orElseThrow();
			assertThat(checker.getCode()).isEqualTo("345678");
		}

		@Test
		@DisplayName("204 sem e-mail quando o e-mail não existe")
		void recoveryIsSilentWhenEmailIsUnknown() throws Exception {
			final var result = recover("ausente.recovery@example.com")
					.andExpect(status().isNoContent())
					.andReturn();
			assertNoContentBody(result);
			if (recordingMailer != null) {
				assertThat(recordingMailer.sent()).isEmpty();
			}
		}

		@Test
		@DisplayName("204 sem e-mail quando o CHANGE_PASSWORD tem mais de 30 minutos")
		void recoveryIsSilentWhenCheckerIsExpired() throws Exception {
			persistExpiredChangePassword(CARLA_ID, "123456");
			if (recordingMailer != null) {
				recordingMailer.clear();
			}

			recover("carla@example.com").andExpect(status().isNoContent());

			if (recordingMailer != null) {
				assertThat(recordingMailer.sent()).isEmpty();
			}
			final var checker = checkerRepository
					.findByProfileIdAndType(CARLA_ID, Checker.Type.CHANGE_PASSWORD)
					.orElseThrow();
			assertThat(checker.getCode()).isEqualTo("123456");
		}

		@ParameterizedTest
		@ValueSource(strings = { "Basic dXNlcjpwYXNz", "Bearer not-a-jwt" })
		@DisplayName("204 mesmo com Authorization Basic ou Bearer inválido: rota pública ignora o header")
		void recoveryIgnoresAuthorizationHeader(final String authorization) throws Exception {
			publicMvc.perform(post(Routes.PROFILE + "/password/recovery")
					.header("Authorization", authorization)
					.contentType(MediaType.APPLICATION_JSON)
					.content(recoveryBody("bruno@example.com"))
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNoContent());
		}

		@Test
		@DisplayName("400 quando o e-mail está mal formado")
		void recoveryRejectsMalformedEmail() throws Exception {
			final var result = recover("not-an-email")
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "email", "well-formed email");
		}

		@Test
		@DisplayName("429 no pedido depois do teto, com Retry-After, mesmo para e-mail inexistente")
		void recoveryReturns429WhenLimitIsExceeded() throws Exception {
			MvcResult last = null;
			for (int i = 0; i < 6; i++) {
				last = recover("ausente.recovery.limit@example.com").andReturn();
			}
			assertThat(last.getResponse().getStatus()).isEqualTo(429);
			assertThat(Integer.parseInt(last.getResponse().getHeader(HttpHeaders.RETRY_AFTER))).isPositive();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(last));
			assertThat(jsonObjectKeys(n)).containsExactly("credentials");
			assertThat(n.get("credentials").get(0).asText()).contains("wait");
		}

		@Test
		@DisplayName("204 confirma o código, troca a senha e encerra as sessões")
		void confirmHashesWipesAndDeletesChecker() throws Exception {
			final var session = IntegrationAuth.openSession(webApplicationContext, BRUNO_ID, false);
			recover("bruno@example.com").andExpect(status().isNoContent());
			final var code = checkerRepository
					.findByProfileIdAndType(BRUNO_ID, Checker.Type.CHANGE_PASSWORD)
					.orElseThrow()
					.getCode();

			final var result = confirm("bruno@example.com", code, "novaSenhaSegura1")
					.andExpect(status().isNoContent())
					.andReturn();
			assertNoContentBody(result);
			final var persisted = profileRepository.findById(BRUNO_ID).orElseThrow();
			assertThat(persisted.getPassword()).startsWith("$2a$");
			assertThat(persisted.getPassword()).isNotEqualTo(PASSWORD_HASH);
			assertThat(checkerRepository.findByProfileIdAndType(BRUNO_ID, Checker.Type.CHANGE_PASSWORD)).isEmpty();
			publicMvc.perform(get(Routes.PROFILE + "/" + BRUNO_ID)
					.header("Authorization", "Bearer " + session.access().value())
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isUnauthorized());
		}

		@Test
		@DisplayName("401 quando o código diverge e o vigente não muda")
		void confirmRejectsWrongCodeWithoutRotating() throws Exception {
			recover("bruno@example.com").andExpect(status().isNoContent());
			final var before = checkerRepository
					.findByProfileIdAndType(BRUNO_ID, Checker.Type.CHANGE_PASSWORD)
					.orElseThrow();

			final var result = confirm("bruno@example.com", "000000", "novaSenhaSegura1")
					.andExpect(status().isUnauthorized())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("code");
			assertThat(n.get("code").get(0).asText()).contains("verification code");
			final var after = checkerRepository
					.findByProfileIdAndType(BRUNO_ID, Checker.Type.CHANGE_PASSWORD)
					.orElseThrow();
			assertThat(after.getCode()).isEqualTo(before.getCode());
			assertThat(profileRepository.findById(BRUNO_ID).orElseThrow().getPassword()).isEqualTo(PASSWORD_HASH);
		}

		@Test
		@DisplayName("401 quando o e-mail não existe")
		void confirmRejectsUnknownEmail() throws Exception {
			final var result = confirm("ausente.confirm@example.com", "123456", "novaSenhaSegura1")
					.andExpect(status().isUnauthorized())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("code");
		}

		@Test
		@DisplayName("401 quando o CHANGE_PASSWORD tem mais de 30 minutos")
		void confirmRejectsExpiredChecker() throws Exception {
			persistExpiredChangePassword(CARLA_ID, "123456");

			final var result = confirm("carla@example.com", "123456", "novaSenhaSegura1")
					.andExpect(status().isUnauthorized())
					.andReturn();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(n)).containsExactly("code");
			assertThat(profileRepository.findById(CARLA_ID).orElseThrow().getPassword()).isEqualTo(PASSWORD_HASH);
			assertThat(checkerRepository.findByProfileIdAndType(CARLA_ID, Checker.Type.CHANGE_PASSWORD)).isPresent();
		}

		@Test
		@DisplayName("400 quando o código está mal formado")
		void confirmRejectsMalformedCode() throws Exception {
			final var result = confirm("bruno@example.com", "12a456", "novaSenhaSegura1")
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "code", "6 digits");
		}

		@Test
		@DisplayName("400 quando a senha nova é curta")
		void confirmRejectsShortNewPassword() throws Exception {
			final var result = confirm("bruno@example.com", "123456", "1234567")
					.andExpect(status().isBadRequest())
					.andReturn();
			assertBadRequestSingleProperty(result, "newPassword", "between");
		}

		@Test
		@DisplayName("429 na confirmação depois do teto, com Retry-After")
		void confirmReturns429WhenLimitIsExceeded() throws Exception {
			MvcResult last = null;
			for (int i = 0; i < 6; i++) {
				last = confirm("ausente.confirm.limit@example.com", "123456", "novaSenhaSegura1").andReturn();
			}
			assertThat(last.getResponse().getStatus()).isEqualTo(429);
			assertThat(Integer.parseInt(last.getResponse().getHeader(HttpHeaders.RETRY_AFTER))).isPositive();
			final JsonNode n = objectMapper.readTree(responseBodyUtf8(last));
			assertThat(jsonObjectKeys(n)).containsExactly("credentials");
			assertThat(n.get("credentials").get(0).asText()).contains("wait");
		}

		@ParameterizedTest
		@ValueSource(strings = { "Basic dXNlcjpwYXNz", "Bearer not-a-jwt" })
		@DisplayName("Confirmação pública ignora Authorization Basic ou Bearer inválido")
		void confirmIgnoresAuthorizationHeader(final String authorization) throws Exception {
			recover("bruno@example.com").andExpect(status().isNoContent());
			final var code = checkerRepository
					.findByProfileIdAndType(BRUNO_ID, Checker.Type.CHANGE_PASSWORD)
					.orElseThrow()
					.getCode();

			publicMvc.perform(post(Routes.PROFILE + "/password/confirm")
					.header("Authorization", authorization)
					.contentType(MediaType.APPLICATION_JSON)
					.content(confirmBody("bruno@example.com", code, "novaSenhaSegura1"))
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isNoContent());
		}

		private org.springframework.test.web.servlet.ResultActions recover(final String email) throws Exception {
			return publicMvc.perform(post(Routes.PROFILE + "/password/recovery")
					.contentType(MediaType.APPLICATION_JSON)
					.content(recoveryBody(email))
					.accept(MediaType.APPLICATION_JSON));
		}

		private org.springframework.test.web.servlet.ResultActions confirm(
				final String email,
				final String code,
				final String newPassword) throws Exception {
			return publicMvc.perform(post(Routes.PROFILE + "/password/confirm")
					.contentType(MediaType.APPLICATION_JSON)
					.content(confirmBody(email, code, newPassword))
					.accept(MediaType.APPLICATION_JSON));
		}

		private static String recoveryBody(final String email) {
			return """
					{
					  "email": "%s"
					}
					""".formatted(email);
		}

		private static String confirmBody(final String email, final String code, final String newPassword) {
			return """
					{
					  "email": "%s",
					  "code": "%s",
					  "newPassword": "%s"
					}
					""".formatted(email, code, newPassword);
		}

		private void persistExpiredChangePassword(final UUID profileId, final String code) {
			checkerRepository.save(CheckerJpaEntity.builder()
					.id(Checker.uuidV7At(Instant.now().minus(Duration.ofMinutes(31))))
					.profileId(profileId)
					.type(Checker.Type.CHANGE_PASSWORD)
					.code(code)
					.build());
			checkerRepository.flush();
		}
	}
}
