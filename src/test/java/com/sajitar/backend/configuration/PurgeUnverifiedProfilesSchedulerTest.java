package com.sajitar.backend.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import com.sajitar.backend.application.usecase.profile.PurgeExpiredChangeEmailCheckersUseCase;
import com.sajitar.backend.application.usecase.profile.PurgeExpiredChangePasswordCheckersUseCase;
import com.sajitar.backend.application.usecase.profile.PurgeUnverifiedProfilesUseCase;
import com.sajitar.backend.domain.exception.SessionStoreUnavailableException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;

@ExtendWith(OutputCaptureExtension.class)
@DisplayName("PurgeUnverifiedProfilesScheduler")
class PurgeUnverifiedProfilesSchedulerTest {

    @BeforeEach
    void captureErrorLog() {
        routeErrorLogToStdout(PurgeUnverifiedProfilesScheduler.class);
    }

    @Test
    @DisplayName("Delega a varredura aos use cases")
    void delegatesToUseCases() {
        final var unverified = mock(PurgeUnverifiedProfilesUseCase.class);
        final var changePassword = mock(PurgeExpiredChangePasswordCheckersUseCase.class);
        final var changeEmail = mock(PurgeExpiredChangeEmailCheckersUseCase.class);
        final var scheduler = new PurgeUnverifiedProfilesScheduler(unverified, changePassword, changeEmail);

        scheduler.execute();

        verify(unverified).execute();
        verify(changePassword).execute();
        verify(changeEmail).execute();
    }

    @Test
    @DisplayName("Falha no lote registra a causa e não segue para os use cases seguintes")
    void logsAndStopsWhenAUseCaseFails(final CapturedOutput output) {
        final var unverified = mock(PurgeUnverifiedProfilesUseCase.class);
        final var changePassword = mock(PurgeExpiredChangePasswordCheckersUseCase.class);
        final var changeEmail = mock(PurgeExpiredChangeEmailCheckersUseCase.class);
        doThrow(new SessionStoreUnavailableException()).when(unverified).execute();
        final var scheduler = new PurgeUnverifiedProfilesScheduler(unverified, changePassword, changeEmail);

        final var thrown = catchThrowable(scheduler::execute);

        assertThat(thrown).isInstanceOf(SessionStoreUnavailableException.class);
        assertThat(output).contains("Unverified profile purge failed");
        verify(changePassword, never()).execute();
        verify(changeEmail, never()).execute();
    }

    private static void routeErrorLogToStdout(final Class<?> type) {
        if (!(org.slf4j.LoggerFactory.getILoggerFactory() instanceof LoggerContext context)) {
            return;
        }
        final var logger = context.getLogger(type);
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
