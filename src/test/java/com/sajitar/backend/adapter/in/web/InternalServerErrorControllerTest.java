package com.sajitar.backend.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.boot.autoconfigure.web.ErrorProperties.IncludeAttribute.ALWAYS;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver;
import org.springframework.boot.webmvc.error.DefaultErrorAttributes;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import jakarta.servlet.RequestDispatcher;

@ExtendWith(OutputCaptureExtension.class)
@DisplayName("InternalServerErrorController")
class InternalServerErrorControllerTest {

    private static final String MARKER = "sajitar-stack-probe-7f1a9c";

    private InternalServerErrorController controller;

    @BeforeEach
    void setUp() {
        final var properties = new WebProperties();
        properties.getError().setIncludeStacktrace(ALWAYS);
        properties.getError().setIncludeMessage(ALWAYS);
        properties.getError().setIncludeException(true);
        final ObjectProvider<ErrorViewResolver> resolvers = mock();
        when(resolvers.orderedStream()).thenReturn(Stream.empty());
        controller = new InternalServerErrorController(new DefaultErrorAttributes(), properties, resolvers);
        routeErrorLogToStdout();
    }

    @ParameterizedTest(name = "{0}, trace={1}")
    @CsvSource({
            "json, false",
            "json, true",
            "html, false",
            "html, true"
    })
    @DisplayName("500 responde sem corpo e sem stack mesmo com trace")
    void internalServerErrorHasEmptyBody(final String format, final boolean trace, final CapturedOutput output)
            throws Exception {
        final var body = bodyOf(500, "html".equals(format), true, trace);

        assertThat(body).isEmpty();
        assertThat(body).doesNotContain(MARKER, "IllegalStateException", "Whitelabel", "InternalServerErrorController");
        assertThat(output).contains("Internal server error", MARKER);
    }

    @Test
    @DisplayName("500 sem exceção responde sem corpo e registra a linha fixa")
    void internalServerErrorWithoutExceptionHasEmptyBody(final CapturedOutput output) throws Exception {
        final var body = bodyOf(500, false, false, true);

        assertThat(body).isEmpty();
        assertThat(output).contains("Internal server error");
        assertThat(output.toString()).doesNotContain(MARKER);
    }

    @Test
    @DisplayName("404 JSON não inclui stack trace mesmo com include-stacktrace always e trace=true")
    void notFoundJsonOmitsStackTrace() {
        final var request = errorRequest(404, true, true);
        final var response = controller.error(request);

        assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).doesNotContainKey("trace");
        assertThat(response.getBody().toString()).doesNotContain("\tat ", "Whitelabel");
        assertThat(controller.isIncludeStackTrace(request, MediaType.ALL)).isFalse();
    }

    @Test
    @DisplayName("404 HTML não inclui stack trace nem a página Whitelabel no model")
    void notFoundHtmlOmitsStackTrace() {
        final var request = errorRequest(404, true, true);
        final var response = new MockHttpServletResponse();

        final var mav = controller.errorHtml(request, response);

        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(mav).isNotNull();
        assertThat(mav.getViewName()).isEqualTo("error");
        assertThat(mav.getModel()).doesNotContainKey("trace");
        assertThat(String.valueOf(mav.getModel())).doesNotContain("\tat ", "Whitelabel");
    }

    private String bodyOf(final int status, final boolean html, final boolean withException, final boolean trace)
            throws Exception {
        final var request = errorRequest(status, withException, trace);
        if (html) {
            final var response = new MockHttpServletResponse();
            final var mav = controller.errorHtml(request, response);
            assertThat(mav).isNull();
            assertThat(response.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR.value());
            return response.getContentAsString();
        }
        final var response = controller.error(request);
        assertThat(response.getStatusCode()).isEqualTo(INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNull();
        return "";
    }

    private static MockHttpServletRequest errorRequest(
            final int status,
            final boolean withException,
            final boolean trace) {
        final var request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, status);
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/profiles");
        if (withException) {
            request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, new IllegalStateException(MARKER));
        }
        if (trace) {
            request.setParameter("trace", "true");
        }
        return request;
    }

    private static void routeErrorLogToStdout() {
        if (!(org.slf4j.LoggerFactory.getILoggerFactory() instanceof LoggerContext context)) {
            return;
        }
        final var logger = context.getLogger(InternalServerErrorController.class);
        logger.detachAndStopAllAppenders();
        final var encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern("%msg%n%ex");
        encoder.start();
        final var appender = new ConsoleAppender<ILoggingEvent>();
        appender.setContext(context);
        appender.setEncoder(encoder);
        appender.setTarget("System.out");
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.ERROR);
        logger.setAdditive(false);
    }

}
