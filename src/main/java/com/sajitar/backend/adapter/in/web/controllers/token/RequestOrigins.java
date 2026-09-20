package com.sajitar.backend.adapter.in.web.controllers.token;

import java.util.Objects;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import com.sajitar.backend.configuration.AttemptProperties;
import com.sajitar.backend.domain.model.token.Client;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;

@Component
@RequiredArgsConstructor
class RequestOrigins {

    private static final String FORWARDED_FOR = "X-Forwarded-For";

    private static final String UNKNOWN = "Unknown";

    private static final String HACKER = "Hacker";

    private final AttemptProperties properties;

    private final UserAgentAnalyzer analyzer;

    String address(final HttpServletRequest request) {
        if (properties.trustForwardedFor()) {
            final var forwarded = firstForwarded(request.getHeader(FORWARDED_FOR));
            if (forwarded != null) {
                return forwarded;
            }
        }
        return Objects.requireNonNullElse(request.getRemoteAddr(), "");
    }

    Client client(final HttpServletRequest request) {
        return parse(request.getHeader(HttpHeaders.USER_AGENT));
    }

    Client parse(final String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return null;
        }
        final var parsed = analyzer.parse(userAgent);
        final var name = parsed.getValue(UserAgent.AGENT_NAME);
        if (useless(name)) {
            return null;
        }
        return new Client(name, useful(parsed.getValue(UserAgent.OPERATING_SYSTEM_NAME)),
                device(parsed.getValue(UserAgent.DEVICE_CLASS)));
    }

    static Client.Device device(final String deviceClass) {
        if (deviceClass == null) {
            return Client.Device.UNKNOWN;
        }
        return switch (deviceClass) {
            case "Desktop" -> Client.Device.DESKTOP;
            case "Phone", "Mobile" -> Client.Device.MOBILE;
            case "Tablet" -> Client.Device.TABLET;
            default -> Client.Device.UNKNOWN;
        };
    }

    private static String firstForwarded(final String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        final var first = header.split(",", 2)[0].trim();
        return first.isEmpty() ? null : first;
    }

    static String useful(final String value) {
        return useless(value) ? null : value;
    }

    static boolean useless(final String value) {
        return value == null || value.isBlank() || UNKNOWN.equals(value) || HACKER.equals(value);
    }

}
