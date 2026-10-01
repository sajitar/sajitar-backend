package com.sajitar.backend.application.usecase.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sajitar.backend.application.command.PatchValue;
import com.sajitar.backend.application.command.profile.PatchProfileCommand;
import com.sajitar.backend.domain.exception.ForbiddenProfileTypeException;
import com.sajitar.backend.domain.exception.ForbiddenProfileUpdateException;
import com.sajitar.backend.domain.exception.InvalidCredentialsException;
import com.sajitar.backend.domain.exception.ProfileNotFoundException;
import com.sajitar.backend.domain.exception.TooManyAttemptsException;
import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.model.profile.Profile;
import com.sajitar.backend.domain.model.token.AttemptScope;
import com.sajitar.backend.domain.port.PasswordHasher;
import com.sajitar.backend.domain.port.checker.CheckerRepository;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.port.token.AttemptLimiter;
import com.sajitar.backend.domain.validation.Limit;
import com.sajitar.backend.domain.validation.profile.Birthday;
import com.sajitar.backend.domain.validation.profile.Description;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@ExtendWith(MockitoExtension.class)
@DisplayName("PatchProfileUseCase")
class PatchProfileUseCaseTest {

    @Mock
    private ProfileRepository profiles;

    @Mock
    private CheckerRepository checkers;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private AttemptLimiter attempts;

    private PatchProfileUseCase useCase;

    @BeforeAll
    static void configureValidation() {
        Birthday.BirthdayValidator.configure(18);
        Limit.LimitValidator.configure(100);
    }

    @BeforeEach
    void setUp() {
        useCase = new PatchProfileUseCase(
                profiles, checkers, passwordHasher, attempts, ProfileUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Atualiza só o nome e mantém o id persistido")
    void patchesOnlyName() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new PatchProfileCommand(
                existing.id(),
                null,
                PatchValue.of("Nome Atualizado"),
                null,
                null,
                null);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(profiles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command, existing.id());

        assertThat(saved.id()).isEqualTo(existing.id());
        assertThat(saved.name()).isEqualTo("Nome Atualizado");
        assertThat(saved.description()).isEqualTo(existing.description());
        assertThat(saved.birthday()).isEqualTo(existing.birthday());
        assertThat(saved.email()).isEqualTo(existing.email());
        assertThat(saved.password()).isEqualTo(existing.password());
        assertThat(saved.type()).isEqualTo(existing.type());
        verify(profiles, never()).findByEmail(any());
    }

    @Test
    @DisplayName("Atualiza só a descrição")
    void patchesOnlyDescription() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new PatchProfileCommand(
                existing.id(),
                null,
                null,
                PatchValue.of("Nova descricao"),
                null,
                null);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(profiles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command, existing.id());

        assertThat(saved.description()).isEqualTo("Nova descricao");
        assertThat(saved.name()).isEqualTo(existing.name());
    }

    @Test
    @DisplayName("description nula remove a descrição atual")
    void clearsDescriptionWhenPresentNull() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new PatchProfileCommand(
                existing.id(),
                null,
                null,
                PatchValue.of(null),
                null,
                null);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(profiles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command, existing.id());

        assertThat(saved.description()).isNull();
    }

    @Test
    @DisplayName("Lança ProfileNotFoundException quando o id não existe")
    void throwsWhenProfileDoesNotExist() {
        final var command = ProfileUseCaseFixture.emptyPatchCommand();
        when(profiles.findById(command.id())).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID));

        assertThat(thrown).isInstanceOf(ProfileNotFoundException.class);
        verify(profiles).findById(command.id());
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("Nome inválido presente não consulta o repositório")
    void doesNotTouchRepositoryWhenPresentNameIsInvalid() {
        final var command = new PatchProfileCommand(
                ProfileUseCaseFixture.ID,
                null,
                PatchValue.of("123"),
                null,
                null,
                null);

        final var thrown = catchThrowable(() -> useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(Pattern.class);
        verify(profiles, never()).findById(any());
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("Patch vazio persiste o existente com o mesmo id")
    void emptyPatchPersistsExistingWithSameId() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(profiles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(ProfileUseCaseFixture.emptyPatchCommand(), ProfileUseCaseFixture.VIEWER_ID);

        assertThat(saved).isEqualTo(existing);
        assertThat(saved.id()).isEqualTo(existing.id());
        assertThat(saved.name()).isEqualTo(existing.name());
        assertThat(saved.description()).isEqualTo(existing.description());
        assertThat(saved.birthday()).isEqualTo(existing.birthday());
        assertThat(saved.email()).isEqualTo(existing.email());
        assertThat(saved.password()).isEqualTo(existing.password());
        assertThat(saved.type()).isEqualTo(existing.type());
        final var captor = ArgumentCaptor.forClass(com.sajitar.backend.domain.model.profile.Profile.class);
        verify(profiles).save(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo(existing.id());
    }

    @Test
    @DisplayName("Atualiza só a data de nascimento")
    void patchesOnlyBirthday() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var birthday = LocalDate.parse("1980-05-20");
        final var command = new PatchProfileCommand(
                existing.id(),
                null,
                null,
                null,
                PatchValue.of(birthday),
                null);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(profiles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command, existing.id());

        assertThat(saved.birthday()).isEqualTo(birthday);
        assertThat(saved.name()).isEqualTo(existing.name());
    }

    @Test
    @DisplayName("Id nulo: violação @NotNull no command")
    void rejectsNullId() {
        final var command = new PatchProfileCommand(null, null, null, null, null, null);

        final var thrown = catchThrowable(() -> useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotNull.class);
        verify(profiles, never()).findById(any());
    }

    @Test
    @DisplayName("Descrição presente inválida não consulta o repositório")
    void doesNotTouchRepositoryWhenPresentDescriptionIsInvalid() {
        final var command = new PatchProfileCommand(
                ProfileUseCaseFixture.ID,
                null,
                null,
                PatchValue.of("x".repeat(Description.MAX_SIZE + 1)),
                null,
                null);

        final var thrown = catchThrowable(() -> useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findById(any());
    }

    @Test
    @DisplayName("Nascimento presente abaixo da idade mínima não consulta o repositório")
    void doesNotTouchRepositoryWhenPresentBirthdayIsInvalid() {
        final var command = new PatchProfileCommand(
                ProfileUseCaseFixture.ID,
                null,
                null,
                null,
                PatchValue.of(LocalDate.now().minusYears(10)),
                null);

        final var thrown = catchThrowable(() -> useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findById(any());
    }

    @Test
    @DisplayName("Atualiza só o tipo e mantém os demais campos")
    void patchesOnlyType() {
        final var existing = ProfileUseCaseFixture.persistedProfile().withTwoFactor(true);
        final var viewer = ProfileUseCaseFixture.persistedMaster(ProfileUseCaseFixture.VIEWER_ID);
        final var command = crossingCommand(existing.id(), Profile.Type.MASTER, null);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(profiles.findById(ProfileUseCaseFixture.VIEWER_ID)).thenReturn(Optional.of(viewer));
        allowCallerPassword(viewer);
        when(profiles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID);

        assertThat(saved.type()).isEqualTo(Profile.Type.MASTER);
        assertThat(saved.name()).isEqualTo(existing.name());
        assertThat(saved.description()).isEqualTo(existing.description());
        assertThat(saved.birthday()).isEqualTo(existing.birthday());
        assertThat(saved.password()).isEqualTo(existing.password());
    }

    @Test
    @DisplayName("Caller que não é MASTER não troca o tipo")
    void nonMasterCannotChangeType() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new PatchProfileCommand(
                existing.id(),
                Profile.Type.MASTER,
                null,
                null,
                null,
                null);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(profiles.findById(ProfileUseCaseFixture.VIEWER_ID))
                .thenReturn(Optional.of(ProfileUseCaseFixture.persistedProfile().withId(ProfileUseCaseFixture.VIEWER_ID)));

        final var thrown = catchThrowable(() -> useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID));

        assertThat(thrown).isInstanceOf(ForbiddenProfileTypeException.class);
        assertThat(((ForbiddenProfileTypeException) thrown).content().get("type"))
                .containsExactly(ForbiddenProfileTypeException.MESSAGE_KEY);
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("Viewer nulo não troca o tipo")
    void nullViewerCannotChangeType() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new PatchProfileCommand(
                existing.id(),
                Profile.Type.WRITER,
                null,
                null,
                null,
                null);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));

        final var thrown = catchThrowable(() -> useCase.execute(command, null));

        assertThat(thrown).isInstanceOf(ForbiddenProfileTypeException.class);
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("Viewer inexistente não troca o tipo")
    void missingViewerCannotChangeType() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new PatchProfileCommand(
                existing.id(),
                Profile.Type.MASTER,
                null,
                null,
                null,
                null);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(profiles.findById(ProfileUseCaseFixture.VIEWER_ID)).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID));

        assertThat(thrown).isInstanceOf(ForbiddenProfileTypeException.class);
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("Caller que não é MASTER persiste quando o type é o vigente")
    void nonMasterKeepsSameType() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new PatchProfileCommand(
                existing.id(),
                existing.type(),
                PatchValue.of("Nome Atualizado"),
                null,
                null,
                null);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(profiles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command, existing.id());

        assertThat(saved.name()).isEqualTo("Nome Atualizado");
        assertThat(saved.type()).isEqualTo(existing.type());
        verify(profiles, never()).findById(ProfileUseCaseFixture.VIEWER_ID);
    }

    @Test
    @DisplayName("Caller alheio não troca o nome")
    void strangerCannotChangeName() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new PatchProfileCommand(
                existing.id(),
                null,
                PatchValue.of("Nome Atualizado"),
                null,
                null,
                null);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));

        final var thrown = catchThrowable(() -> useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID));

        assertThat(thrown).isInstanceOf(ForbiddenProfileUpdateException.class);
        assertThat(((ForbiddenProfileUpdateException) thrown).content().get("id"))
                .containsExactly(ForbiddenProfileUpdateException.MESSAGE_KEY);
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("Caller alheio MASTER não troca o nome")
    void masterStrangerCannotChangeName() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new PatchProfileCommand(
                existing.id(),
                null,
                PatchValue.of("Nome Atualizado"),
                null,
                null,
                null);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));

        final var thrown = catchThrowable(() -> useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID));

        assertThat(thrown).isInstanceOf(ForbiddenProfileUpdateException.class);
        verify(profiles, never()).save(any());
        verify(profiles, never()).findById(ProfileUseCaseFixture.VIEWER_ID);
    }

    @Test
    @DisplayName("Caller alheio não troca a descrição")
    void strangerCannotChangeDescription() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new PatchProfileCommand(
                existing.id(),
                null,
                null,
                PatchValue.of("Nova descricao"),
                null,
                null);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));

        final var thrown = catchThrowable(() -> useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID));

        assertThat(thrown).isInstanceOf(ForbiddenProfileUpdateException.class);
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("Caller alheio não limpa a descrição")
    void strangerCannotClearDescription() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new PatchProfileCommand(
                existing.id(),
                null,
                null,
                PatchValue.of(null),
                null,
                null);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));

        final var thrown = catchThrowable(() -> useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID));

        assertThat(thrown).isInstanceOf(ForbiddenProfileUpdateException.class);
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("Caller alheio não troca a data de nascimento")
    void strangerCannotChangeBirthday() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new PatchProfileCommand(
                existing.id(),
                null,
                null,
                null,
                PatchValue.of(existing.birthday().minusYears(1)),
                null);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));

        final var thrown = catchThrowable(() -> useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID));

        assertThat(thrown).isInstanceOf(ForbiddenProfileUpdateException.class);
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("Caller alheio que muda atributo e type recebe 403 {id}")
    void strangerChangingAttributesAndTypeGetsIdForbidden() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new PatchProfileCommand(
                existing.id(),
                Profile.Type.MASTER,
                PatchValue.of("Nome Atualizado"),
                null,
                null,
                null);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));

        final var thrown = catchThrowable(() -> useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID));

        assertThat(thrown).isInstanceOf(ForbiddenProfileUpdateException.class);
        verify(profiles, never()).save(any());
        verify(profiles, never()).findById(ProfileUseCaseFixture.VIEWER_ID);
    }

    @Test
    @DisplayName("Dono que não é MASTER e muda nome e type recebe 403 {type}")
    void ownerNonMasterChangingNameAndTypeGetsTypeForbidden() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new PatchProfileCommand(
                existing.id(),
                Profile.Type.MASTER,
                PatchValue.of("Nome Atualizado"),
                null,
                null,
                null);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));

        final var thrown = catchThrowable(() -> useCase.execute(command, existing.id()));

        assertThat(thrown).isInstanceOf(ForbiddenProfileTypeException.class);
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("Viewer nulo não troca atributos")
    void nullViewerCannotChangeAttributes() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new PatchProfileCommand(
                existing.id(),
                null,
                PatchValue.of("Nome Atualizado"),
                null,
                null,
                null);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));

        final var thrown = catchThrowable(() -> useCase.execute(command, null));

        assertThat(thrown).isInstanceOf(ForbiddenProfileUpdateException.class);
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("twoFactor nulo presente não consulta o repositório")
    void doesNotTouchRepositoryWhenPresentTwoFactorIsNull() {
        final var command = new PatchProfileCommand(
                ProfileUseCaseFixture.ID,
                null,
                null,
                null,
                null,
                PatchValue.of(null));

        final var thrown = catchThrowable(() -> useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).findById(any());
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("Atualiza só o twoFactor")
    void patchesOnlyTwoFactor() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new PatchProfileCommand(
                existing.id(),
                null,
                null,
                null,
                null,
                PatchValue.of(true));
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(profiles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command, existing.id());

        assertThat(saved.twoFactor()).isTrue();
        assertThat(saved.name()).isEqualTo(existing.name());
        verify(checkers, never()).deleteById(any());
    }

    @Test
    @DisplayName("Caller alheio não troca twoFactor")
    void strangerCannotChangeTwoFactor() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new PatchProfileCommand(
                existing.id(),
                null,
                null,
                null,
                null,
                PatchValue.of(true));
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));

        final var thrown = catchThrowable(() -> useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID));

        assertThat(thrown).isInstanceOf(ForbiddenProfileUpdateException.class);
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("Desligar twoFactor em quem não é MASTER apaga o SIGN_IN")
    void disablingTwoFactorDeletesSignInChecker() {
        final var existing = ProfileUseCaseFixture.persistedProfile().withTwoFactor(true);
        final var checker = Checker.create(existing.id(), Checker.Type.SIGN_IN);
        final var command = new PatchProfileCommand(
                existing.id(),
                null,
                null,
                null,
                null,
                PatchValue.of(false));
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(profiles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(checkers.findByProfileIdAndType(existing.id(), Checker.Type.SIGN_IN)).thenReturn(Optional.of(checker));

        useCase.execute(command, existing.id());

        verify(checkers).deleteById(checker.id());
    }

    @Test
    @DisplayName("MASTER com twoFactor false não grava")
    void masterWithTwoFactorFalseDoesNotSave() {
        final var existing = ProfileUseCaseFixture.persistedMaster(ProfileUseCaseFixture.ID);
        final var command = new PatchProfileCommand(
                existing.id(),
                null,
                null,
                null,
                null,
                PatchValue.of(false));
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));

        final var thrown = catchThrowable(() -> useCase.execute(command, existing.id()));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        verify(profiles, never()).save(any());
        verify(checkers, never()).findByProfileIdAndType(any(), any());
    }

    @Test
    @DisplayName("Rebaixar MASTER com twoFactor false apaga o SIGN_IN")
    void demotingMasterWithTwoFactorFalseDeletesSignInChecker() {
        final var existing = ProfileUseCaseFixture.persistedMaster(ProfileUseCaseFixture.ID);
        final var checker = Checker.create(existing.id(), Checker.Type.SIGN_IN);
        final var command = crossingCommand(existing.id(), Profile.Type.WRITER, PatchValue.of(false));
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        allowCallerPassword(existing);
        when(profiles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(checkers.findByProfileIdAndType(existing.id(), Checker.Type.SIGN_IN)).thenReturn(Optional.of(checker));

        useCase.execute(command, existing.id());

        verify(checkers).deleteById(checker.id());
    }

    @Test
    @DisplayName("MASTER troca WRITER por READER sem senha")
    void masterChangesWriterToReaderWithoutPassword() {
        final var existing = ProfileUseCaseFixture.persistedProfile().withType(Profile.Type.WRITER);
        final var viewer = ProfileUseCaseFixture.persistedMaster(ProfileUseCaseFixture.VIEWER_ID);
        final var command = new PatchProfileCommand(
                existing.id(),
                Profile.Type.READER,
                null,
                null,
                null,
                null);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(profiles.findById(ProfileUseCaseFixture.VIEWER_ID)).thenReturn(Optional.of(viewer));
        when(profiles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID);

        assertThat(saved.type()).isEqualTo(Profile.Type.READER);
        verify(passwordHasher, never()).matches(any(), any());
        verify(attempts, never()).register(any(), any());
    }

    @Test
    @DisplayName("Password extra fora da fronteira MASTER não é conferida")
    void extraPasswordOutsideMasterBoundaryIsIgnored() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new PatchProfileCommand(
                existing.id(),
                null,
                PatchValue.of("Nome Atualizado"),
                null,
                null,
                null,
                "senhaErrada1",
                ProfileUseCaseFixture.ADDRESS);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(profiles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command, existing.id());

        assertThat(saved.name()).isEqualTo("Nome Atualizado");
        verify(passwordHasher, never()).matches(any(), any());
        verify(attempts, never()).register(any(), any());
    }

    @Test
    @DisplayName("Caller que não é MASTER com senha ainda recebe 403 {type}")
    void nonMasterWithPasswordStillCannotChangeType() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = crossingCommand(existing.id(), Profile.Type.MASTER, null);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(profiles.findById(ProfileUseCaseFixture.VIEWER_ID))
                .thenReturn(Optional.of(ProfileUseCaseFixture.persistedProfile().withId(ProfileUseCaseFixture.VIEWER_ID)));

        final var thrown = catchThrowable(() -> useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID));

        assertThat(thrown).isInstanceOf(ForbiddenProfileTypeException.class);
        verify(profiles, never()).save(any());
        verify(attempts, never()).register(any(), any());
        verify(passwordHasher, never()).matches(any(), any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "", "   ", "1234567" })
    @DisplayName("Fronteira MASTER sem senha bem formada barra antes do limiter")
    void masterBoundaryRequiresWellFormedPassword(final String password) {
        final var existing = ProfileUseCaseFixture.persistedProfile().withTwoFactor(true);
        final var viewer = ProfileUseCaseFixture.persistedMaster(ProfileUseCaseFixture.VIEWER_ID);
        final var command = new PatchProfileCommand(
                existing.id(),
                Profile.Type.MASTER,
                null,
                null,
                null,
                null,
                password,
                ProfileUseCaseFixture.ADDRESS);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(profiles.findById(ProfileUseCaseFixture.VIEWER_ID)).thenReturn(Optional.of(viewer));

        final var thrown = catchThrowable(() -> useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getPropertyPath()).hasToString("password");
        verify(attempts, never()).register(any(), any());
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("Senha do caller que não confere não grava o type")
    void refusesWrongCallerPasswordOnMasterBoundary() {
        final var existing = ProfileUseCaseFixture.persistedProfile().withTwoFactor(true);
        final var viewer = ProfileUseCaseFixture.persistedMaster(ProfileUseCaseFixture.VIEWER_ID);
        final var command = crossingCommand(existing.id(), Profile.Type.MASTER, null);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(profiles.findById(ProfileUseCaseFixture.VIEWER_ID)).thenReturn(Optional.of(viewer));
        when(attempts.register(AttemptScope.CREDENTIALS, ProfileUseCaseFixture.ADDRESS)).thenReturn(Optional.empty());
        when(attempts.register(AttemptScope.CREDENTIALS, viewer.email())).thenReturn(Optional.empty());
        when(passwordHasher.matches(ProfileUseCaseFixture.PASSWORD, viewer.password())).thenReturn(false);

        final var thrown = catchThrowable(() -> useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID));

        assertThat(thrown).isInstanceOf(InvalidCredentialsException.class);
        assertThat(((InvalidCredentialsException) thrown).content().get("credentials"))
                .containsExactly(InvalidCredentialsException.MESSAGE_KEY);
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("Limite por endereço barra antes de conferir a senha na fronteira MASTER")
    void refusesWhenAddressLimitIsExceededOnMasterBoundary() {
        final var existing = ProfileUseCaseFixture.persistedProfile().withTwoFactor(true);
        final var viewer = ProfileUseCaseFixture.persistedMaster(ProfileUseCaseFixture.VIEWER_ID);
        final var command = crossingCommand(existing.id(), Profile.Type.MASTER, null);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(profiles.findById(ProfileUseCaseFixture.VIEWER_ID)).thenReturn(Optional.of(viewer));
        when(attempts.register(AttemptScope.CREDENTIALS, ProfileUseCaseFixture.ADDRESS))
                .thenReturn(Optional.of(Duration.ofSeconds(12)));
        when(attempts.register(AttemptScope.CREDENTIALS, viewer.email())).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID));

        assertThat(thrown).isInstanceOf(TooManyAttemptsException.class);
        assertThat(((TooManyAttemptsException) thrown).retryAfterSeconds()).isEqualTo(12L);
        verify(passwordHasher, never()).matches(any(), any());
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("Limite por e-mail do caller barra na fronteira MASTER")
    void refusesWhenEmailLimitIsExceededOnMasterBoundary() {
        final var existing = ProfileUseCaseFixture.persistedProfile().withTwoFactor(true);
        final var viewer = ProfileUseCaseFixture.persistedMaster(ProfileUseCaseFixture.VIEWER_ID);
        final var command = crossingCommand(existing.id(), Profile.Type.MASTER, null);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(profiles.findById(ProfileUseCaseFixture.VIEWER_ID)).thenReturn(Optional.of(viewer));
        when(attempts.register(AttemptScope.CREDENTIALS, ProfileUseCaseFixture.ADDRESS)).thenReturn(Optional.empty());
        when(attempts.register(AttemptScope.CREDENTIALS, viewer.email()))
                .thenReturn(Optional.of(Duration.ofMillis(1500)));

        final var thrown = catchThrowable(() -> useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID));

        assertThat(thrown).isInstanceOf(TooManyAttemptsException.class);
        assertThat(((TooManyAttemptsException) thrown).retryAfterSeconds()).isEqualTo(2L);
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("Quando endereço e e-mail estouram na fronteira MASTER, a espera é a maior")
    void usesLongerWaitWhenBothLimitsAreExceededOnMasterBoundary() {
        final var existing = ProfileUseCaseFixture.persistedProfile().withTwoFactor(true);
        final var viewer = ProfileUseCaseFixture.persistedMaster(ProfileUseCaseFixture.VIEWER_ID);
        final var command = crossingCommand(existing.id(), Profile.Type.MASTER, null);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(profiles.findById(ProfileUseCaseFixture.VIEWER_ID)).thenReturn(Optional.of(viewer));
        when(attempts.register(AttemptScope.CREDENTIALS, ProfileUseCaseFixture.ADDRESS))
                .thenReturn(Optional.of(Duration.ofSeconds(3)));
        when(attempts.register(AttemptScope.CREDENTIALS, viewer.email()))
                .thenReturn(Optional.of(Duration.ofSeconds(9)));

        final var thrown = catchThrowable(() -> useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID));

        assertThat(thrown).isInstanceOf(TooManyAttemptsException.class);
        assertThat(((TooManyAttemptsException) thrown).retryAfterSeconds()).isEqualTo(9L);
    }

    private void allowCallerPassword(final Profile viewer) {
        when(attempts.register(AttemptScope.CREDENTIALS, ProfileUseCaseFixture.ADDRESS)).thenReturn(Optional.empty());
        when(attempts.register(AttemptScope.CREDENTIALS, viewer.email())).thenReturn(Optional.empty());
        when(passwordHasher.matches(ProfileUseCaseFixture.PASSWORD, viewer.password())).thenReturn(true);
    }

    private static PatchProfileCommand crossingCommand(
            final UUID id,
            final Profile.Type type,
            final PatchValue<Boolean> twoFactor) {
        return new PatchProfileCommand(
                id,
                type,
                null,
                null,
                null,
                twoFactor,
                ProfileUseCaseFixture.PASSWORD,
                ProfileUseCaseFixture.ADDRESS);
    }

}
