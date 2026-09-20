package com.sajitar.backend.controller;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.io.IOException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sajitar.backend.domain.model.token.IssuedSession;
import com.sajitar.backend.domain.model.token.Session;
import com.sajitar.backend.domain.port.token.SessionStore;
import com.sajitar.backend.domain.port.token.TokenIssuer;
import com.sajitar.backend.settlement.profile.ProfileSettlementFixture;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

final class IntegrationAuth {

    private IntegrationAuth() {
    }

    static MockMvc withSecurity(final WebApplicationContext context) {
        return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    static MockMvc withSecurityAndAliceBearer(final WebApplicationContext context) {
        final var session = openSession(context, ProfileSettlementFixture.ALICE_ID, false);
        return MockMvcBuilders.webAppContextSetup(context)
                .addFilter(new BearerHeaderFilter(session.access().value()))
                .apply(springSecurity())
                .build();
    }

    /** Abre uma sessão real no store: o Bearer só autentica com registro ativo. */
    static IssuedSession openSession(
            final WebApplicationContext context,
            final UUID profileId,
            final boolean withRefresh) {
        final var issuer = context.getBean(TokenIssuer.class);
        final var sessions = context.getBean(SessionStore.class);
        final var now = context.getBean(Clock.class).instant();
        final var access = issuer.issueAccess(now);
        final var opened = Session.open(profileId, access.id(), null);
        final var refresh = withRefresh ? issuer.issueRefresh(now, opened.bornAt()) : null;
        final var session = refresh == null ? opened : opened.withRefresh(refresh.id());
        sessions.open(session, access.claims(), refresh == null ? null : refresh.claims(), null);
        return new IssuedSession(session.id(), access, refresh);
    }

    /** Refresh assinado e válido, porém sem registro de sessão no store. */
    static String danglingRefreshToken(final WebApplicationContext context) {
        final var now = context.getBean(Clock.class).instant();
        return context.getBean(TokenIssuer.class).issueRefresh(now, now).value();
    }

    private static final class BearerHeaderFilter extends OncePerRequestFilter {

        private final String authorization;

        private BearerHeaderFilter(final String token) {
            this.authorization = "Bearer " + token;
        }

        @Override
        protected void doFilterInternal(
                final HttpServletRequest request,
                final HttpServletResponse response,
                final FilterChain filterChain) throws ServletException, IOException {
            var effective = request;
            if (request.getHeader(HttpHeaders.AUTHORIZATION) == null) {
                effective = new HttpServletRequestWrapper(request) {
                    @Override
                    public String getHeader(final String name) {
                        if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)) {
                            return authorization;
                        }
                        return super.getHeader(name);
                    }

                    @Override
                    public java.util.Enumeration<String> getHeaders(final String name) {
                        if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)) {
                            return Collections.enumeration(List.of(authorization));
                        }
                        return super.getHeaders(name);
                    }

                    @Override
                    public java.util.Enumeration<String> getHeaderNames() {
                        final var names = new ArrayList<String>();
                        super.getHeaderNames().asIterator().forEachRemaining(names::add);
                        if (names.stream().noneMatch(HttpHeaders.AUTHORIZATION::equalsIgnoreCase)) {
                            names.add(HttpHeaders.AUTHORIZATION);
                        }
                        return Collections.enumeration(names);
                    }
                };
            }
            filterChain.doFilter(effective, response);
        }
    }

}
