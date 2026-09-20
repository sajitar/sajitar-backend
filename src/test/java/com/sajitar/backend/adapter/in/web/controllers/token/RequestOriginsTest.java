package com.sajitar.backend.adapter.in.web.controllers.token;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

import com.sajitar.backend.configuration.AttemptPropertiesFixture;
import com.sajitar.backend.domain.model.token.Client;

import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;

@DisplayName("RequestOrigins")
class RequestOriginsTest {

    private static final String REMOTE = "198.51.100.20";

    private static final String FORWARDED = "203.0.113.10";

    private static UserAgentAnalyzer analyzer;

    @BeforeAll
    static void loadAnalyzer() {
        analyzer = UserAgentAnalyzer.newBuilder()
                .hideMatcherLoadStats()
                .withCache(10_000)
                .withField(UserAgent.AGENT_NAME)
                .withField(UserAgent.OPERATING_SYSTEM_NAME)
                .withField(UserAgent.DEVICE_CLASS)
                .build();
    }

    @ParameterizedTest(name = "{1}")
    @CsvSource(delimiter = '|', textBlock = """
            Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 | Chrome Linux | Chrome | Linux | DESKTOP
            Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0 | Firefox Windows | Firefox | Windows NT | DESKTOP
            Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15 | Safari Mac OS | Safari | Mac OS | DESKTOP
            Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0 | Edge Windows | Edge | Windows NT | DESKTOP
            Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.140 Safari/537.36 Edge/18.17763 | Edge legado | Edge | Windows NT | DESKTOP
            Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 OPR/106.0.0.0 | Opera Windows | Opera | Windows NT | DESKTOP
            Opera/9.80 (Windows NT 6.1; WOW64) Presto/2.12.388 Version/12.18 | Opera Presto | Opera | Windows NT | DESKTOP
            Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1 | Safari iPhone | Safari | iOS | MOBILE
            Mozilla/5.0 (iPad; CPU OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1 | Safari iPad | Safari | iOS | TABLET
            Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 | Chrome Android | Chrome | Android | MOBILE
            Mozilla/5.0 (Linux; Android 13; SM-X810) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 | Chrome tablet Android | Chrome | Android | TABLET
            Mozilla/5.0 (Linux; Android 13; Pixel Tablet) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36 | Tablet na UA | Chrome | Android | TABLET
            """)
    @DisplayName("Parseia User-Agents reais com o YAUAA")
    void parsesKnownUserAgents(
            final String userAgent,
            final String label,
            final String name,
            final String os,
            final Client.Device device) {
        final var request = request(userAgent, REMOTE, null);

        final var client = origins(false).client(request);

        assertThat(client).as(label).isEqualTo(new Client(name, os, device));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   ", "???" })
    @DisplayName("Sem User-Agent ou lixo devolve null")
    void unknownUserAgentIsOmitted(final String userAgent) {
        assertThat(origins(false).parse(userAgent)).isNull();
        assertThat(origins(false).client(request(userAgent, REMOTE, null))).isNull();
    }

    @Test
    @DisplayName("PostmanRuntime vira client com name Postman")
    void recognizesPostman() {
        final var client = origins(false).parse("PostmanRuntime/7.43.0");

        assertThat(client).isNotNull();
        assertThat(client.name()).isEqualTo("Postman Runtime");
        assertThat(client.device()).isEqualTo(Client.Device.UNKNOWN);
    }

    @Test
    @DisplayName("curl vira client com name curl")
    void recognizesCurl() {
        final var client = origins(false).parse("curl/8.5.0");

        assertThat(client).isNotNull();
        assertThat(client.name()).isEqualTo("Curl");
        assertThat(client.device()).isEqualTo(Client.Device.UNKNOWN);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "Unknown", "Hacker" })
    @DisplayName("AgentName e OS inúteis são descartados")
    void treatsUnknownAndHackerAsUseless(final String value) {
        assertThat(RequestOrigins.useless(value)).isTrue();
        assertThat(RequestOrigins.useful(value)).isNull();
    }

    @Test
    @DisplayName("Campo útil permanece")
    void keepsUsefulField() {
        assertThat(RequestOrigins.useless("Chrome")).isFalse();
        assertThat(RequestOrigins.useful("Chrome")).isEqualTo("Chrome");
    }

    @ParameterizedTest
    @CsvSource({
            "Desktop, DESKTOP",
            "Phone, MOBILE",
            "Mobile, MOBILE",
            "Tablet, TABLET",
            "Robot, UNKNOWN"
    })
    @DisplayName("DeviceClass do YAUAA cai no enum da API")
    void mapsDeviceClass(final String deviceClass, final Client.Device device) {
        assertThat(RequestOrigins.device(deviceClass)).isEqualTo(device);
    }

    @Test
    @DisplayName("DeviceClass nulo vira UNKNOWN")
    void unknownDeviceWhenClassIsNull() {
        assertThat(RequestOrigins.device(null)).isEqualTo(Client.Device.UNKNOWN);
    }

    @Test
    @DisplayName("Sem confiar no proxy, o endereço é o remoteAddr")
    void usesRemoteAddrWhenForwardedForIsUntrusted() {
        final var request = request(null, REMOTE, FORWARDED + ", 10.0.0.1");

        assertThat(origins(false).address(request)).isEqualTo(REMOTE);
    }

    @Test
    @DisplayName("Com proxy confiável, usa o primeiro endereço de X-Forwarded-For")
    void usesFirstForwardedAddressWhenTrusted() {
        final var request = request(null, REMOTE, FORWARDED + ", 10.0.0.1");

        assertThat(origins(true).address(request)).isEqualTo(FORWARDED);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   ", ", 10.0.0.1" })
    @DisplayName("X-Forwarded-For vazio cai no remoteAddr mesmo com proxy confiável")
    void fallsBackToRemoteAddrWhenForwardedIsBlank(final String forwarded) {
        final var request = request(null, REMOTE, forwarded);

        assertThat(origins(true).address(request)).isEqualTo(REMOTE);
    }

    @Test
    @DisplayName("RemoteAddr nulo vira string vazia")
    void emptyWhenRemoteAddrIsMissing() {
        final var request = new MockHttpServletRequest();
        request.setRemoteAddr(null);

        assertThat(origins(false).address(request)).isEmpty();
    }

    private static RequestOrigins origins(final boolean trustForwardedFor) {
        return new RequestOrigins(trustForwardedFor
                ? AttemptPropertiesFixture.trustingForwardedFor()
                : AttemptPropertiesFixture.defaults(), analyzer);
    }

    private static MockHttpServletRequest request(final String userAgent, final String remote, final String forwarded) {
        final var request = new MockHttpServletRequest();
        request.setRemoteAddr(remote);
        if (userAgent != null) {
            request.addHeader(HttpHeaders.USER_AGENT, userAgent);
        }
        if (forwarded != null) {
            request.addHeader("X-Forwarded-For", forwarded);
        }
        return request;
    }

}
