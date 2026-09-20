package com.sajitar.backend.application.usecase.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sajitar.backend.application.command.token.RefreshTokenCommand;
import com.sajitar.backend.domain.exception.EmailNotVerifiedException;
import com.sajitar.backend.domain.exception.InvalidRefreshTokenException;
import com.sajitar.backend.domain.exception.TooManyAttemptsException;
import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.model.token.AttemptScope;
import com.sajitar.backend.domain.model.token.Session;
import com.sajitar.backend.domain.port.checker.CheckerRepository;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.port.token.AttemptLimiter;
import com.sajitar.backend.domain.port.token.RefreshTokenDecoder;
import com.sajitar.backend.domain.port.token.RotationCommand;
import com.sajitar.backend.domain.port.token.RotationOutcome;
import com.sajitar.backend.domain.port.token.SessionStore;
import com.sajitar.backend.domain.port.token.TokenIssuer;
import com.sajitar.backend.domain.validation.Limit;
import com.sajitar.backend.domain.validation.profile.Birthday;

import jakarta.validation.ConstraintViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenUseCase")
class RefreshTokenUseCaseTest {

    private static final String PRESENTED = "eyJhbGciOiJIUzI1NiJ9.refresh.presented";

    @Mock
    private ProfileRepository profiles;

    @Mock
    private CheckerRepository checkers;

    @Mock
    private RefreshTokenDecoder refreshTokens;

    @Mock
    private TokenIssuer tokens;

    @Mock
    private SessionStore sessions;

    @Mock
    private AttemptLimiter attempts;

    private RefreshTokenUseCase useCase;

    private final UUID presentedId = UUID.fromString("018f3c2a-7b00-7c3d-9e1a-000000000002");

    @BeforeAll
    static void configureValidation() {
        Birthday.BirthdayValidator.configure(18);
        Limit.LimitValidator.configure(100);
    }

    @BeforeEach
    void setUp() {
        useCase = new RefreshTokenUseCase(
                profiles,
                checkers,
                refreshTokens,
                tokens,
                sessions,
                attempts,
                TokenUseCaseFixture.CLOCK,
                TokenUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Refresh vigente rotaciona o par mantendo o id da sessão")
    void rotatesCurrentRefresh() {
        final var session = activeSession();
        final var access = TokenUseCaseFixture.access();
        final var refresh = TokenUseCaseFixture.refresh();
        profileFound();
        when(tokens.issueAccess(TokenUseCaseFixture.NOW)).thenReturn(access);
        when(tokens.issueRefresh(TokenUseCaseFixture.NOW, session.bornAt())).thenReturn(refresh);
        when(sessions.rotate(any())).thenReturn(new RotationOutcome.Rotated());

        final var issued = useCase.execute(new RefreshTokenCommand(PRESENTED, TokenUseCaseFixture.ADDRESS, TokenUseCaseFixture.CLIENT));

        assertThat(issued.sessionId()).isEqualTo(session.id());
        assertThat(issued.access()).isEqualTo(access);
        assertThat(issued.refresh()).isEqualTo(refresh);
        final var captor = ArgumentCaptor.forClass(RotationCommand.class);
        verify(sessions).rotate(captor.capture());
        assertThat(captor.getValue().presentedRefreshId()).isEqualTo(presentedId);
        assertThat(captor.getValue().session()).isEqualTo(session);
        assertThat(captor.getValue().access()).isEqualTo(access.claims());
        assertThat(captor.getValue().refresh()).isEqualTo(refresh.claims());
        assertThat(captor.getValue().client()).isEqualTo(TokenUseCaseFixture.CLIENT);
    }

    @Test
    @DisplayName("Corrida na rotação cai na graça e reemite o par sucessor")
    void reissuesSuccessorWhenRotationLosesRace() {
        final var session = activeSession();
        final var successorAccess = TokenUseCaseFixture.access();
        final var successorRefresh = TokenUseCaseFixture.refresh();
        profileFound();
        when(tokens.issueAccess(TokenUseCaseFixture.NOW)).thenReturn(TokenUseCaseFixture.access());
        when(tokens.issueRefresh(TokenUseCaseFixture.NOW, session.bornAt())).thenReturn(TokenUseCaseFixture.refresh());
        when(sessions.rotate(any())).thenReturn(new RotationOutcome.Replayed(
                session.id(),
                successorAccess.claims(),
                successorRefresh.claims()));
        when(tokens.reissue(successorAccess.claims())).thenReturn(successorAccess);
        when(tokens.reissue(successorRefresh.claims())).thenReturn(successorRefresh);

        final var issued = useCase.execute(new RefreshTokenCommand(PRESENTED, TokenUseCaseFixture.ADDRESS, TokenUseCaseFixture.CLIENT));

        assertThat(issued.sessionId()).isEqualTo(session.id());
        assertThat(issued.access()).isEqualTo(successorAccess);
        assertThat(issued.refresh()).isEqualTo(successorRefresh);
    }

    @Test
    @DisplayName("Rotação recusada pelo store vira refresh inválido")
    void rejectsWhenRotationIsInvalid() {
        final var session = activeSession();
        profileFound();
        when(tokens.issueAccess(TokenUseCaseFixture.NOW)).thenReturn(TokenUseCaseFixture.access());
        when(tokens.issueRefresh(TokenUseCaseFixture.NOW, session.bornAt())).thenReturn(TokenUseCaseFixture.refresh());
        when(sessions.rotate(any())).thenReturn(new RotationOutcome.Invalid());

        final var thrown = catchThrowable(() -> useCase.execute(new RefreshTokenCommand(PRESENTED, TokenUseCaseFixture.ADDRESS, TokenUseCaseFixture.CLIENT)));

        assertThat(thrown).isInstanceOf(InvalidRefreshTokenException.class);
        assertThat(((InvalidRefreshTokenException) thrown).content().get("refreshToken"))
                .containsExactly(InvalidRefreshTokenException.MESSAGE_KEY);
    }

    @Test
    @DisplayName("Retry do refresh já consumido devolve o mesmo par sucessor")
    void replaysConsumedRefreshWithinGrace() {
        final var sessionId = UUID.fromString("018f3c2a-7b00-7c3d-9e1a-000000000010");
        final var successorAccess = TokenUseCaseFixture.access();
        final var successorRefresh = TokenUseCaseFixture.refresh();
        when(refreshTokens.refreshId(PRESENTED)).thenReturn(presentedId);
        when(sessions.findActiveRefresh(presentedId)).thenReturn(Optional.empty());
        when(sessions.replay(presentedId)).thenReturn(new RotationOutcome.Replayed(
                sessionId,
                successorAccess.claims(),
                successorRefresh.claims()));
        when(tokens.reissue(successorAccess.claims())).thenReturn(successorAccess);
        when(tokens.reissue(successorRefresh.claims())).thenReturn(successorRefresh);

        final var issued = useCase.execute(new RefreshTokenCommand(PRESENTED, TokenUseCaseFixture.ADDRESS, TokenUseCaseFixture.CLIENT));

        assertThat(issued.sessionId()).isEqualTo(sessionId);
        assertThat(issued.access()).isEqualTo(successorAccess);
        assertThat(issued.refresh()).isEqualTo(successorRefresh);
        verify(sessions, never()).rotate(any());
        verify(profiles, never()).findById(any());
    }

    @Test
    @DisplayName("Refresh sem registro vigente nem tombstone na graça é 401")
    void rejectsRefreshWithoutActiveRecord() {
        when(refreshTokens.refreshId(PRESENTED)).thenReturn(presentedId);
        when(sessions.findActiveRefresh(presentedId)).thenReturn(Optional.empty());
        when(sessions.replay(presentedId)).thenReturn(new RotationOutcome.Invalid());

        final var thrown = catchThrowable(() -> useCase.execute(new RefreshTokenCommand(PRESENTED, TokenUseCaseFixture.ADDRESS, TokenUseCaseFixture.CLIENT)));

        assertThat(thrown).isInstanceOf(InvalidRefreshTokenException.class);
        verify(tokens, never()).issueAccess(any());
    }

    @Test
    @DisplayName("Perfil que sumiu do banco não renova nada")
    void rejectsWhenProfileIsGone() {
        final var session = activeSession();
        when(profiles.findById(session.profileId())).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(new RefreshTokenCommand(PRESENTED, TokenUseCaseFixture.ADDRESS, TokenUseCaseFixture.CLIENT)));

        assertThat(thrown).isInstanceOf(InvalidRefreshTokenException.class);
        verify(sessions, never()).rotate(any());
    }

    @Test
    @DisplayName("Perfil com VERIFY_EMAIL não renova")
    void rejectsUnverifiedEmail() {
        final var session = activeSession();
        when(profiles.findById(session.profileId())).thenReturn(Optional.of(TokenUseCaseFixture.persistedProfile()));
        when(checkers.findByProfileIdAndType(TokenUseCaseFixture.PROFILE_ID, Checker.Type.VERIFY_EMAIL))
                .thenReturn(Optional.of(TokenUseCaseFixture.verifyEmailChecker()));

        final var thrown = catchThrowable(() -> useCase.execute(new RefreshTokenCommand(PRESENTED, TokenUseCaseFixture.ADDRESS, TokenUseCaseFixture.CLIENT)));

        assertThat(thrown).isInstanceOf(EmailNotVerifiedException.class);
        verify(sessions, never()).rotate(any());
    }

    @Test
    @DisplayName("Sessão no teto absoluto não emite refresh novo")
    void rejectsWhenSessionReachedAbsoluteCeiling() {
        final var session = activeSession();
        profileFound();
        when(tokens.issueAccess(TokenUseCaseFixture.NOW)).thenReturn(TokenUseCaseFixture.access());
        when(tokens.issueRefresh(TokenUseCaseFixture.NOW, session.bornAt()))
                .thenReturn(TokenUseCaseFixture.expiredRefresh());

        final var thrown = catchThrowable(() -> useCase.execute(new RefreshTokenCommand(PRESENTED, TokenUseCaseFixture.ADDRESS, TokenUseCaseFixture.CLIENT)));

        assertThat(thrown).isInstanceOf(InvalidRefreshTokenException.class);
        verify(sessions, never()).rotate(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   " })
    @DisplayName("Refresh em branco barra antes de decodificar")
    void validatesBeforePorts(final String refreshToken) {
        final var thrown = catchThrowable(() -> useCase.execute(
                new RefreshTokenCommand(refreshToken, TokenUseCaseFixture.ADDRESS, TokenUseCaseFixture.CLIENT)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(attempts, never()).register(any(), any());
        verify(refreshTokens, never()).refreshId(any());
        verify(sessions, never()).findActiveRefresh(any());
    }

    @Test
    @DisplayName("Limite por endereço barra antes de decodificar o refresh")
    void refusesWhenAddressLimitIsExceeded() {
        when(attempts.register(AttemptScope.REFRESH, TokenUseCaseFixture.ADDRESS))
                .thenReturn(Optional.of(Duration.ofSeconds(8)));

        final var thrown = catchThrowable(() -> useCase.execute(
                new RefreshTokenCommand(PRESENTED, TokenUseCaseFixture.ADDRESS, TokenUseCaseFixture.CLIENT)));

        assertThat(thrown).isInstanceOf(TooManyAttemptsException.class);
        assertThat(((TooManyAttemptsException) thrown).content().get("refreshToken"))
                .containsExactly(TooManyAttemptsException.MESSAGE_KEY);
        verify(refreshTokens, never()).refreshId(any());
        verify(sessions, never()).findActiveRefresh(any());
    }

    private Session activeSession() {
        final var session = Session.open(TokenUseCaseFixture.PROFILE_ID, UUID.randomUUID(), presentedId);
        when(refreshTokens.refreshId(PRESENTED)).thenReturn(presentedId);
        when(sessions.findActiveRefresh(presentedId)).thenReturn(Optional.of(session));
        return session;
    }

    private void profileFound() {
        when(profiles.findById(TokenUseCaseFixture.PROFILE_ID))
                .thenReturn(Optional.of(TokenUseCaseFixture.persistedProfile()));
        when(checkers.findByProfileIdAndType(TokenUseCaseFixture.PROFILE_ID, Checker.Type.VERIFY_EMAIL))
                .thenReturn(Optional.empty());
    }

}
