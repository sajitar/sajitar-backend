package com.sajitar.backend.adapter.in.web.controller.checker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sajitar.backend.adapter.in.web.controller.IntegrationAuth;

/**
 * A superfície HTTP de {@code /checkers} foi retirada: sem Bearer responde 401;
 * com access válido não há mapping.
 */
@SpringBootTest
@DisplayName("/checkers (superfície HTTP retirada)")
class CheckerRouteIntegrationTest {

	private static final String CHECKERS = "/checkers";

	@Autowired
	private WebApplicationContext webApplicationContext;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private static String responseBodyUtf8(final MvcResult result) {
		return new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
	}

	private Set<String> jsonObjectKeys(final JsonNode node) {
		final var keys = new HashSet<String>();
		node.fieldNames().forEachRemaining(keys::add);
		return keys;
	}

	@Test
	@DisplayName("GET sem Bearer responde 401 {token}")
	void getWithoutBearerReturns401() throws Exception {
		final MockMvc anonymous = IntegrationAuth.withSecurity(webApplicationContext);
		final MvcResult result = anonymous.perform(get(CHECKERS).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized())
				.andReturn();
		final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
		assertThat(jsonObjectKeys(n)).containsExactly("token");
		assertThat(n.get("token").get(0).asText()).contains("bearer token");
	}

	@Test
	@DisplayName("POST sem Bearer responde 401 {token}")
	void postWithoutBearerReturns401() throws Exception {
		final MockMvc anonymous = IntegrationAuth.withSecurity(webApplicationContext);
		final MvcResult result = anonymous.perform(post(CHECKERS)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}")
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized())
				.andReturn();
		final JsonNode n = objectMapper.readTree(responseBodyUtf8(result));
		assertThat(jsonObjectKeys(n)).containsExactly("token");
		assertThat(n.get("token").get(0).asText()).contains("bearer token");
	}

	@Test
	@DisplayName("GET com Bearer da Alice responde 404")
	void getWithAliceBearerReturns404() throws Exception {
		final MockMvc mockMvc = IntegrationAuth.withSecurityAndAliceBearer(webApplicationContext);
		mockMvc.perform(get(CHECKERS).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("POST com Bearer da Alice responde 404")
	void postWithAliceBearerReturns404() throws Exception {
		final MockMvc mockMvc = IntegrationAuth.withSecurityAndAliceBearer(webApplicationContext);
		mockMvc.perform(post(CHECKERS)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}")
				.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());
	}

}
