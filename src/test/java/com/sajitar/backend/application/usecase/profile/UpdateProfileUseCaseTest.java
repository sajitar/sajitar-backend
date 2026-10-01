package com.sajitar.backend.application.usecase.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sajitar.backend.application.command.profile.UpdateProfileCommand;
import com.sajitar.backend.domain.exception.ForbiddenProfileTypeException;
import com.sajitar.backend.domain.exception.ForbiddenProfileUpdateException;
import com.sajitar.backend.domain.exception.ProfileNotFoundException;
import com.sajitar.backend.domain.model.checker.Checker;
import com.sajitar.backend.domain.model.profile.Profile;
import com.sajitar.backend.domain.port.checker.CheckerRepository;
import com.sajitar.backend.domain.port.profile.ProfileRepository;
import com.sajitar.backend.domain.validation.Limit;
import com.sajitar.backend.domain.validation.profile.Birthday;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateProfileUseCase")
class UpdateProfileUseCaseTest {

    @Mock
    private ProfileRepository profiles;

    @Mock
    private CheckerRepository checkers;

    private UpdateProfileUseCase useCase;

    @BeforeAll
    static void configureValidation() {
        Birthday.BirthdayValidator.configure(18);
        Limit.LimitValidator.configure(100);
    }

    @BeforeEach
    void setUp() {
        useCase = new UpdateProfileUseCase(profiles, checkers, ProfileUseCaseFixture.VALIDATOR);
    }

    @Test
    @DisplayName("Mantém o hash da senha e o e-mail ao atualizar os demais campos")
    void keepsExistingPasswordAndEmail() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = ProfileUseCaseFixture.validUpdateCommand();
        when(profiles.findById(command.id())).thenReturn(Optional.of(existing));
        when(profiles.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID);

        assertThat(saved.password()).isEqualTo(existing.password());
        assertThat(saved.email()).isEqualTo(existing.email());
        assertThat(saved.type()).isEqualTo(existing.type());
        verify(profiles).save(any(Profile.class));
        verify(profiles, never()).findByEmail(any());
    }

    @Test
    @DisplayName("Lança ProfileNotFoundException quando o id não existe")
    void throwsWhenProfileDoesNotExist() {
        final var command = ProfileUseCaseFixture.validUpdateCommand();
        when(profiles.findById(command.id())).thenReturn(Optional.empty());

        final var thrown = catchThrowable(() -> useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID));

        assertThat(thrown).isInstanceOf(ProfileNotFoundException.class);
        verify(profiles).findById(command.id());
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("Persiste o nome novo mantendo id, senha e e-mail")
    void persistsWhenUpdatingSameProfile() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new UpdateProfileCommand(
                existing.id(),
                existing.type(),
                "Nome Atualizado",
                existing.description(),
                existing.birthday(),
                false);
        when(profiles.findById(command.id())).thenReturn(Optional.of(existing));
        when(profiles.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command, existing.id());

        assertThat(saved.name()).isEqualTo("Nome Atualizado");
        final var captor = ArgumentCaptor.forClass(Profile.class);
        verify(profiles).save(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo(existing.id());
        assertThat(captor.getValue().password()).isEqualTo(existing.password());
        assertThat(captor.getValue().email()).isEqualTo(existing.email());
    }

    @Test
    @DisplayName("Substitui o tipo vigente")
    void replacesType() {
        final var existing = ProfileUseCaseFixture.persistedProfile().withTwoFactor(true);
        final var command = new UpdateProfileCommand(
                existing.id(),
                Profile.Type.MASTER,
                existing.name(),
                existing.description(),
                existing.birthday(),
                true);
        when(profiles.findById(command.id())).thenReturn(Optional.of(existing));
        when(profiles.findById(ProfileUseCaseFixture.VIEWER_ID))
                .thenReturn(Optional.of(ProfileUseCaseFixture.persistedMaster(ProfileUseCaseFixture.VIEWER_ID)));
        when(profiles.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID);

        assertThat(saved.type()).isEqualTo(Profile.Type.MASTER);
        assertThat(saved.name()).isEqualTo(existing.name());
    }

    @Test
    @DisplayName("Caller que não é MASTER não troca o tipo")
    void nonMasterCannotChangeType() {
        final var existing = ProfileUseCaseFixture.persistedProfile().withTwoFactor(true);
        final var command = new UpdateProfileCommand(
                existing.id(),
                Profile.Type.MASTER,
                existing.name(),
                existing.description(),
                existing.birthday(),
                true);
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
        final var existing = ProfileUseCaseFixture.persistedProfile().withTwoFactor(true);
        final var command = new UpdateProfileCommand(
                existing.id(),
                Profile.Type.MASTER,
                existing.name(),
                existing.description(),
                existing.birthday(),
                true);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));

        final var thrown = catchThrowable(() -> useCase.execute(command, null));

        assertThat(thrown).isInstanceOf(ForbiddenProfileTypeException.class);
        verify(profiles).findById(existing.id());
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("Viewer inexistente não troca o tipo")
    void missingViewerCannotChangeType() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new UpdateProfileCommand(
                existing.id(),
                Profile.Type.WRITER,
                existing.name(),
                existing.description(),
                existing.birthday(),
                false);
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
        final var command = new UpdateProfileCommand(
                existing.id(),
                existing.type(),
                "Nome Atualizado",
                existing.description(),
                existing.birthday(),
                false);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));
        when(profiles.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command, existing.id());

        assertThat(saved.name()).isEqualTo("Nome Atualizado");
        assertThat(saved.type()).isEqualTo(existing.type());
        verify(profiles).findById(existing.id());
        verify(profiles, never()).findById(ProfileUseCaseFixture.VIEWER_ID);
    }

    @Test
    @DisplayName("Caller alheio não troca o nome")
    void strangerCannotChangeName() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new UpdateProfileCommand(
                existing.id(),
                existing.type(),
                "Nome Atualizado",
                existing.description(),
                existing.birthday(),
                false);
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
        final var command = new UpdateProfileCommand(
                existing.id(),
                existing.type(),
                "Nome Atualizado",
                existing.description(),
                existing.birthday(),
                false);
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
        final var command = new UpdateProfileCommand(
                existing.id(),
                existing.type(),
                existing.name(),
                "Nova descricao",
                existing.birthday(),
                false);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));

        final var thrown = catchThrowable(() -> useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID));

        assertThat(thrown).isInstanceOf(ForbiddenProfileUpdateException.class);
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("Caller alheio não troca a data de nascimento")
    void strangerCannotChangeBirthday() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new UpdateProfileCommand(
                existing.id(),
                existing.type(),
                existing.name(),
                existing.description(),
                existing.birthday().minusYears(1),
                false);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));

        final var thrown = catchThrowable(() -> useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID));

        assertThat(thrown).isInstanceOf(ForbiddenProfileUpdateException.class);
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("Caller alheio que muda atributo e type recebe 403 {id}")
    void strangerChangingAttributesAndTypeGetsIdForbidden() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new UpdateProfileCommand(
                existing.id(),
                Profile.Type.MASTER,
                "Nome Atualizado",
                existing.description(),
                existing.birthday(),
                true);
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
        final var command = new UpdateProfileCommand(
                existing.id(),
                Profile.Type.MASTER,
                "Nome Atualizado",
                existing.description(),
                existing.birthday(),
                true);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));

        final var thrown = catchThrowable(() -> useCase.execute(command, existing.id()));

        assertThat(thrown).isInstanceOf(ForbiddenProfileTypeException.class);
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("Viewer nulo não troca atributos")
    void nullViewerCannotChangeAttributes() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new UpdateProfileCommand(
                existing.id(),
                existing.type(),
                "Nome Atualizado",
                existing.description(),
                existing.birthday(),
                false);
        when(profiles.findById(existing.id())).thenReturn(Optional.of(existing));

        final var thrown = catchThrowable(() -> useCase.execute(command, null));

        assertThat(thrown).isInstanceOf(ForbiddenProfileUpdateException.class);
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("Tipo nulo: não consulta o repositório")
    void doesNotTouchRepositoryWhenTypeIsNull() {
        final var command = new UpdateProfileCommand(
                ProfileUseCaseFixture.ID,
                null,
                ProfileUseCaseFixture.NAME,
                ProfileUseCaseFixture.DESCRIPTION,
                ProfileUseCaseFixture.BIRTHDAY,
                false);

        final var thrown = catchThrowable(() -> useCase.execute(command, ProfileUseCaseFixture.VIEWER_ID));

        assertThat(thrown).isInstanceOf(jakarta.validation.ConstraintViolationException.class);
        verify(profiles, never()).findById(any());
        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("Dono liga twoFactor")
    void ownerEnablesTwoFactor() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new UpdateProfileCommand(
                existing.id(),
                existing.type(),
                existing.name(),
                existing.description(),
                existing.birthday(),
                true);
        when(profiles.findById(command.id())).thenReturn(Optional.of(existing));
        when(profiles.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final var saved = useCase.execute(command, existing.id());

        assertThat(saved.twoFactor()).isTrue();
        verify(checkers, never()).deleteById(any());
    }

    @Test
    @DisplayName("Caller alheio não troca twoFactor")
    void strangerCannotChangeTwoFactor() {
        final var existing = ProfileUseCaseFixture.persistedProfile();
        final var command = new UpdateProfileCommand(
                existing.id(),
                existing.type(),
                existing.name(),
                existing.description(),
                existing.birthday(),
                true);
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
        final var command = new UpdateProfileCommand(
                existing.id(),
                existing.type(),
                existing.name(),
                existing.description(),
                existing.birthday(),
                false);
        when(profiles.findById(command.id())).thenReturn(Optional.of(existing));
        when(profiles.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(checkers.findByProfileIdAndType(existing.id(), Checker.Type.SIGN_IN)).thenReturn(Optional.of(checker));

        useCase.execute(command, existing.id());

        verify(checkers).deleteById(checker.id());
    }

    @Test
    @DisplayName("MASTER com twoFactor false não grava")
    void masterWithTwoFactorFalseDoesNotSave() {
        final var existing = ProfileUseCaseFixture.persistedMaster(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        final var command = new UpdateProfileCommand(
                existing.id(),
                existing.type(),
                existing.name(),
                existing.description(),
                existing.birthday(),
                false);

        final var thrown = catchThrowable(() -> useCase.execute(command, existing.id()));

        assertThat(thrown).isInstanceOf(jakarta.validation.ConstraintViolationException.class);
        verify(profiles, never()).findById(any());
        verify(profiles, never()).save(any());
        verify(checkers, never()).findByProfileIdAndType(any(), any());
    }

    @Test
    @DisplayName("Rebaixar MASTER com twoFactor false apaga o SIGN_IN")
    void demotingMasterWithTwoFactorFalseDeletesSignInChecker() {
        final var existing = ProfileUseCaseFixture.persistedMaster(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
        final var checker = Checker.create(existing.id(), Checker.Type.SIGN_IN);
        final var command = new UpdateProfileCommand(
                existing.id(),
                Profile.Type.WRITER,
                existing.name(),
                existing.description(),
                existing.birthday(),
                false);
        when(profiles.findById(command.id())).thenReturn(Optional.of(existing));
        when(profiles.save(any(Profile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(checkers.findByProfileIdAndType(existing.id(), Checker.Type.SIGN_IN)).thenReturn(Optional.of(checker));

        useCase.execute(command, existing.id());

        verify(checkers).deleteById(checker.id());
    }

}
