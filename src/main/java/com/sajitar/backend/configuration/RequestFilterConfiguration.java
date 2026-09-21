package com.sajitar.backend.configuration;

import static org.springframework.http.HttpMethod.POST;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

import com.sajitar.backend.adapter.in.web.BearerAuthenticationEntryPoint;
import com.sajitar.backend.adapter.in.web.BearerSessionAuthenticationFilter;
import com.sajitar.backend.adapter.in.web.Routes;
import com.sajitar.backend.domain.port.token.AccessTokenDecoder;
import com.sajitar.backend.domain.port.token.SessionStore;

@Configuration
@EnableWebSecurity
class RequestFilterConfiguration {

    @Bean
    @Order(1)
    SecurityFilterChain publicFilterChain(final HttpSecurity http) throws Exception {
        return http
                .securityMatchers(matchers -> matchers
                        .requestMatchers(POST, Routes.PROFILE)
                        .requestMatchers(POST, Routes.TOKEN + "/signin", Routes.TOKEN + "/refresh",
                                Routes.TOKEN + "/verification")
                        .requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**")
                        .requestMatchers("/actuator", "/actuator/**"))
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain apiFilterChain(
            final HttpSecurity http,
            final BearerAuthenticationEntryPoint entryPoint,
            final AccessTokenDecoder accessTokens,
            final SessionStore sessions) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(entryPoint))
                .addFilterBefore(
                        new BearerSessionAuthenticationFilter(accessTokens, sessions),
                        AuthorizationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .build();
    }

}
