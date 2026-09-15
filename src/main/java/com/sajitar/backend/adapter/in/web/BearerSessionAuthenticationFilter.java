package com.sajitar.backend.adapter.in.web;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sajitar.backend.domain.exception.SessionStoreUnavailableException;
import com.sajitar.backend.domain.port.token.AccessTokenDecoder;
import com.sajitar.backend.domain.port.token.SessionStore;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Autentica o Bearer das rotas protegidas. Assinatura válida não basta: o
 * {@code jti} precisa ter registro ativo e vigente no store de sessões, e o
 * principal é o perfil lido de lá — o JWT não carrega id de perfil.
 * <p>
 * Token ausente ou inválido segue sem autenticação, e quem responde 401 é o
 * {@link BearerAuthenticationEntryPoint}. Store fora do ar é 503: sem consultar
 * as sessões, nenhum acesso é liberado.
 */
@RequiredArgsConstructor
public class BearerSessionAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";

    private final AccessTokenDecoder accessTokens;

    private final SessionStore sessions;

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {
        final var header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(PREFIX)) {
            final var accessId = accessTokens.accessId(header.substring(PREFIX.length())).orElse(null);
            if (accessId != null) {
                try {
                    sessions.profileIdOfActiveAccess(accessId).ifPresent(BearerSessionAuthenticationFilter::authenticate);
                } catch (final SessionStoreUnavailableException _) {
                    response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
                    return;
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private static void authenticate(final Object profileId) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(profileId, null, List.of()));
    }

}
