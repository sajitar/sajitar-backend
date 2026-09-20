package com.sajitar.backend.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;

@DisplayName("UserAgentAnalyzerConfiguration")
class UserAgentAnalyzerConfigurationTest {

    @Test
    @DisplayName("Monta analisador que nomeia Chrome")
    void buildsAnalyzerThatNamesChrome() {
        final var analyzer = new UserAgentAnalyzerConfiguration().userAgentAnalyzer();

        assertThat(analyzer).isInstanceOf(UserAgentAnalyzer.class);
        assertThat(analyzer.parse(
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .getValue(UserAgent.AGENT_NAME)).isEqualTo("Chrome");
    }

}
