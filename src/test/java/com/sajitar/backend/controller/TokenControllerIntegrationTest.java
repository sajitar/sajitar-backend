package com.sajitar.backend.controller;

import static com.sajitar.backend.settlement.profile.ProfileSettlementFixture.ALICE_EMAIL;
import static com.sajitar.backend.settlement.profile.ProfileSettlementFixture.ALICE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sajitar.backend.adapter.in.web.Routes;
import com.sajitar.backend.adapter.in.web.token.TokenController;
import com.sajitar.backend.adapter.out.persistence.checker.CheckerJpaEntity;
import com.sajitar.backend.adapter.out.persistence.checker.CheckerJpaRepository;
import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.settlement.token.SessionSettlementFixture;

/**
 * Integração do {@link TokenController} com PostgreSQL (perfis) e Redis
 * (sessões). Cada teste começa com o store de sessões limpo.
 */
@SpringBootTest
@DisplayName("TokenController (integração HTTP + Redis)")
class TokenControllerIntegrationTest {

	private static final String EMAIL = "tokens.nova@example.com";

	private static final String PASSWORD = "senhaSegura1";

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private StringRedisTemplate redis;

	@Autowired
	private CheckerJpaRepository checkerRepository;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		SessionSettlementFixture.clear(redis);
		mockMvc = IntegrationAuth.withSecurity(webApplicationContext);
	}

	@Nested
	@Transactional
	@DisplayName("POST /tokens/signin")
	class SignIn {

		@Test
		@DisplayName("200 com access, jti e sessionId; sem campos de refresh quando não é pedido")
		void returnsAccessOnlyByDefault() throws Exception {
			createProfile();

			final MvcResult result = signIn(EMAIL, PASSWORD, false)
					.andExpect(status().isOk())
					.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
					.andExpect(header().string("Cache-Control", containsString("no-store")))
					.andReturn();

			final JsonNode body = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(body)).containsExactlyInAnyOrder("token", "type", "expiresIn", "id", "sessionId");
			assertThat(body.get("type").asText()).isEqualTo("Bearer");
			assertThat(body.get("expiresIn").asLong()).isEqualTo(3600);
			assertThat(body.get("token").asText().split("\\.")).hasSize(3);
			assertThat(UUID.fromString(body.get("id").asText())).isNotEqualTo(UUID.fromString(body.get("sessionId").asText()));
		}

		@Test
		@DisplayName("200 com o par completo quando o corpo pede refresh")
		void returnsPairWhenRefreshRequested() throws Exception {
			createProfile();

			final MvcResult result = signIn(EMAIL, PASSWORD, true)
					.andExpect(status().isOk())
					.andReturn();

			final JsonNode body = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(jsonObjectKeys(body)).containsExactlyInAnyOrder(
					"token", "type", "expiresIn", "id", "sessionId", "refreshToken", "refreshId", "refreshExpiresIn");
			assertThat(body.get("refreshExpiresIn").asLong()).isEqualTo(604800);
			assertThat(body.get("refreshToken").asText().split("\\.")).hasSize(3);
			assertThat(body.get("refreshId").asText()).isNotEqualTo(body.get("id").asText());
		}

		@Test
		@DisplayName("O access emitido autentica nas rotas protegidas")
		void issuedAccessAuthenticatesProtectedRoutes() throws Exception {
			createProfile();
			final var token = objectMapper.readTree(responseBodyUtf8(signIn(EMAIL, PASSWORD, false)
					.andExpect(status().isOk())
					.andReturn()))
					.get("token")
					.asText();

			mockMvc.perform(get(Routes.PROFILE + "/" + ALICE_ID)
					.header("Authorization", "Bearer " + token)
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk());
		}

		@Test
		@DisplayName("400 quando o e-mail é inválido")
		void returns400WhenEmailIsInvalid() throws Exception {
			final MvcResult result = signIn("not-an-email", PASSWORD, false)
					.andExpect(status().isBadRequest())
					.andExpect(header().string("Cache-Control", containsString("no-store")))
					.andReturn();

			assertSingleProperty(result, "email", "well-formed email");
		}

		@Test
		@DisplayName("401 quando o e-mail não existe")
		void returns401WhenEmailIsUnknown() throws Exception {
			final MvcResult result = signIn("ausente@example.com", PASSWORD, false)
					.andExpect(status().isUnauthorized())
					.andReturn();

			assertSingleProperty(result, "credentials", "valid credentials");
		}

		@Test
		@DisplayName("401 quando a senha está errada")
		void returns401WhenPasswordIsWrong() throws Exception {
			final MvcResult result = signIn(ALICE_EMAIL, "senhaErrada1", false)
					.andExpect(status().isUnauthorized())
					.andReturn();

			assertSingleProperty(result, "credentials", "valid credentials");
		}

		@ParameterizedTest(name = "lang={0}")
		@CsvSource({
				"pt, credenciais válidas",
				"es, credenciales válidas"
		})
		@DisplayName("401 traduz credenciais inválidas conforme lang")
		void unauthorizedCredentialsFollowsLang(final String lang, final String expected) throws Exception {
			final MvcResult result = mockMvc.perform(post(Routes.TOKEN + "/signin")
					.param("lang", lang)
					.contentType(MediaType.APPLICATION_JSON)
					.content(signInBody(ALICE_EMAIL, "senhaErrada1", false))
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isUnauthorized())
					.andReturn();

			assertSingleProperty(result, "credentials", expected);
		}

		@Test
		@DisplayName("403 quando o perfil tem checker VERIFY_EMAIL")
		void returns403WhenVerifyEmailCheckerExists() throws Exception {
			persistVerifyEmailChecker(createProfile());

			final MvcResult result = signIn(EMAIL, PASSWORD, true)
					.andExpect(status().isForbidden())
					.andReturn();

			assertSingleProperty(result, "email", "verified email");
		}

		@ParameterizedTest(name = "{0}")
		@ValueSource(strings = { "Basic dXNlcjpwYXNz", "Bearer not-a-jwt" })
		@DisplayName("200 mesmo com Authorization Basic ou Bearer inválido: rota pública ignora o header")
		void ignoresAuthorizationHeader(final String authorization) throws Exception {
			createProfile();

			mockMvc.perform(post(Routes.TOKEN + "/signin")
					.header("Authorization", authorization)
					.contentType(MediaType.APPLICATION_JSON)
					.content(signInBody(EMAIL, PASSWORD, false))
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk());
		}
	}

	@Nested
	@Transactional
	@DisplayName("POST /tokens/refresh")
	class Refresh {

		@Test
		@DisplayName("200 com par novo na mesma sessão; o par anterior deixa de valer")
		void rotatesPairKeepingSession() throws Exception {
			createProfile();
			final JsonNode first = signInWithRefresh();

			final MvcResult result = refresh(first.get("refreshToken").asText())
					.andExpect(status().isOk())
					.andExpect(header().string("Cache-Control", containsString("no-store")))
					.andReturn();

			final JsonNode rotated = objectMapper.readTree(responseBodyUtf8(result));
			assertThat(rotated.get("sessionId").asText()).isEqualTo(first.get("sessionId").asText());
			assertThat(rotated.get("id").asText()).isNotEqualTo(first.get("id").asText());
			assertThat(rotated.get("refreshId").asText()).isNotEqualTo(first.get("refreshId").asText());
			mockMvc.perform(get(Routes.PROFILE + "/" + ALICE_ID)
					.header("Authorization", "Bearer " + first.get("token").asText())
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isUnauthorized());
			mockMvc.perform(get(Routes.PROFILE + "/" + ALICE_ID)
					.header("Authorization", "Bearer " + rotated.get("token").asText())
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk());
		}

		@Test
		@DisplayName("Retry na janela de graça devolve o mesmo par sucessor")
		void replaysSuccessorWithinGrace() throws Exception {
			createProfile();
			final JsonNode first = signInWithRefresh();
			final JsonNode rotated = objectMapper.readTree(responseBodyUtf8(
					refresh(first.get("refreshToken").asText()).andExpect(status().isOk()).andReturn()));

			final MvcResult retry = refresh(first.get("refreshToken").asText())
					.andExpect(status().isOk())
					.andReturn();

			final JsonNode replayed = objectMapper.readTree(responseBodyUtf8(retry));
			assertThat(replayed.get("id").asText()).isEqualTo(rotated.get("id").asText());
			assertThat(replayed.get("refreshId").asText()).isEqualTo(rotated.get("refreshId").asText());
			assertThat(replayed.get("sessionId").asText()).isEqualTo(rotated.get("sessionId").asText());
		}

		@Test
		@DisplayName("400 quando o refreshToken está em branco")
		void returns400WhenRefreshTokenIsBlank() throws Exception {
			final MvcResult result = refresh("   ")
					.andExpect(status().isBadRequest())
					.andReturn();

			assertSingleProperty(result, "refreshToken", "blank");
		}

		@Test
		@DisplayName("401 quando o refreshToken é lixo")
		void returns401WhenRefreshTokenIsGarbage() throws Exception {
			final MvcResult result = refresh("not-a-jwt")
					.andExpect(status().isUnauthorized())
					.andReturn();

			assertSingleProperty(result, "refreshToken", "refresh token");
		}

		@Test
		@DisplayName("401 quando o access é enviado no lugar do refresh")
		void returns401WhenAccessTokenIsUsedAsRefresh() throws Exception {
			createProfile();
			final var access = signInWithRefresh().get("token").asText();

			final MvcResult result = refresh(access)
					.andExpect(status().isUnauthorized())
					.andReturn();

			assertSingleProperty(result, "refreshToken", "refresh token");
		}

		@Test
		@DisplayName("401 quando o refresh é válido mas não tem sessão no store")
		void returns401WhenRefreshHasNoSession() throws Exception {
			final var dangling = IntegrationAuth.danglingRefreshToken(webApplicationContext);

			final MvcResult result = refresh(dangling)
					.andExpect(status().isUnauthorized())
					.andReturn();

			assertSingleProperty(result, "refreshToken", "refresh token");
		}

		@Test
		@DisplayName("403 quando o perfil ganhou checker VERIFY_EMAIL depois do signin")
		void returns403WhenVerifyEmailCheckerExists() throws Exception {
			final var profileId = createProfile();
			final JsonNode signedIn = signInWithRefresh();
			persistVerifyEmailChecker(profileId);

			final MvcResult result = refresh(signedIn.get("refreshToken").asText())
					.andExpect(status().isForbidden())
					.andReturn();

			assertSingleProperty(result, "email", "verified email");
		}

		@ParameterizedTest(name = "{0}")
		@ValueSource(strings = { "Basic dXNlcjpwYXNz", "Bearer not-a-jwt" })
		@DisplayName("200 mesmo com Authorization Basic ou Bearer inválido: rota pública ignora o header")
		void ignoresAuthorizationHeader(final String authorization) throws Exception {
			createProfile();
			final JsonNode signedIn = signInWithRefresh();

			mockMvc.perform(post(Routes.TOKEN + "/refresh")
					.header("Authorization", authorization)
					.contentType(MediaType.APPLICATION_JSON)
					.content(refreshBody(signedIn.get("refreshToken").asText()))
					.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk());
		}

		private JsonNode signInWithRefresh() throws Exception {
			return objectMapper.readTree(responseBodyUtf8(
					signIn(EMAIL, PASSWORD, true).andExpect(status().isOk()).andReturn()));
		}
	}

	private UUID createProfile() throws Exception {
		final MvcResult created = mockMvc.perform(post(Routes.PROFILE)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "name": "Zaida Tokens",
						  "description": "Perfil para os testes de /tokens.",
						  "birthday": "1990-01-01",
						  "email": "%s",
						  "password": "%s"
						}
						""".formatted(EMAIL, PASSWORD))
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andReturn();
		return UUID.fromString(objectMapper.readTree(responseBodyUtf8(created)).get("id").asText());
	}

	private void persistVerifyEmailChecker(final UUID profileId) {
		final var checker = Checker.create(profileId, Checker.Type.VERIFY_EMAIL);
		checkerRepository.save(CheckerJpaEntity.builder()
				.id(checker.id())
				.profileId(checker.profileId())
				.type(checker.type())
				.code(checker.code())
				.payload(checker.payload())
				.attempts((short) checker.attempts())
				.replaces((short) checker.replaces())
				.updatedAt(checker.updatedAt())
				.build());
		checkerRepository.flush();
	}

	private org.springframework.test.web.servlet.ResultActions signIn(
			final String email,
			final String password,
			final boolean refresh) throws Exception {
		return mockMvc.perform(post(Routes.TOKEN + "/signin")
				.contentType(MediaType.APPLICATION_JSON)
				.content(signInBody(email, password, refresh))
				.accept(MediaType.APPLICATION_JSON));
	}

	private org.springframework.test.web.servlet.ResultActions refresh(final String refreshToken) throws Exception {
		return mockMvc.perform(post(Routes.TOKEN + "/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(refreshBody(refreshToken))
				.accept(MediaType.APPLICATION_JSON));
	}

	private static String signInBody(final String email, final String password, final boolean refresh) {
		return """
				{
				  "email": "%s",
				  "password": "%s",
				  "refresh": %s
				}
				""".formatted(email, password, refresh);
	}

	private static String refreshBody(final String refreshToken) {
		return """
				{
				  "refreshToken": "%s"
				}
				""".formatted(refreshToken);
	}

	private void assertSingleProperty(final MvcResult result, final String property, final String messageSubstring)
			throws Exception {
		assertThat(result.getResponse().getContentType()).contains("json");
		final JsonNode root = objectMapper.readTree(responseBodyUtf8(result));
		assertThat(jsonObjectKeys(root)).containsExactly(property);
		assertThat(root.get(property).get(0).asText()).contains(messageSubstring);
	}

	private static String responseBodyUtf8(final MvcResult result) {
		return new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
	}

	private static Set<String> jsonObjectKeys(final JsonNode node) {
		final var keys = new HashSet<String>();
		node.fieldNames().forEachRemaining(keys::add);
		return keys;
	}

}
