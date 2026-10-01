package com.sajitar.backend.domain.validation.profile;

import static com.sajitar.backend.domain.validation.profile.MasterRequiresTwoFactor.Validation.validate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sajitar.backend.domain.model.profile.Profile;

import jakarta.validation.ConstraintViolationException;

@DisplayName("Anotação @MasterRequiresTwoFactor")
class MasterRequiresTwoFactorTest {

    @Test
    @DisplayName("Aceita MASTER com twoFactor true")
    void acceptsMasterWithTwoFactorEnabled() {
        final var sample = new Sample(Profile.Type.MASTER, true);

        assertThatCode(() -> validate(sample)).doesNotThrowAnyException();
        assertThat(validate(sample)).isEqualTo(sample);
    }

    @Test
    @DisplayName("Aceita WRITER e READER com twoFactor false")
    void acceptsNonMasterWithTwoFactorDisabled() {
        assertThatCode(() -> validate(new Sample(Profile.Type.WRITER, false))).doesNotThrowAnyException();
        assertThatCode(() -> validate(new Sample(Profile.Type.READER, false))).doesNotThrowAnyException();
        assertThatCode(() -> MasterRequiresTwoFactor.Validation.validate(Profile.Type.WRITER, false))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Rejeita MASTER com twoFactor false no campo twoFactor")
    void rejectsMasterWithTwoFactorDisabled() {
        final var thrown = catchThrowable(() -> validate(new Sample(Profile.Type.MASTER, false)));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getPropertyPath()).hasToString("twoFactor");
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType())
                .isEqualTo(MasterRequiresTwoFactor.class);
        assertThat(violation.getMessage()).isEqualTo("must remain enabled for a master profile");
    }

    @Test
    @DisplayName("validate(type, twoFactor) rejeita MASTER desligado")
    void validatePairRejectsMasterDisabled() {
        final var thrown = catchThrowable(() -> MasterRequiresTwoFactor.Validation.validate(Profile.Type.MASTER, false));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("validate(Validator) rejeita MASTER desligado")
    void validateWithValidatorRejectsMasterDisabled() {
        try (final var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            final var thrown = catchThrowable(() -> MasterRequiresTwoFactor.Validation.validate(
                    factory.getValidator(),
                    new Sample(Profile.Type.MASTER, false)));

            assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        }
    }

    @Test
    @DisplayName("Não compara quando type ou twoFactor é nulo")
    void skipsWhenTypeOrTwoFactorIsNull() {
        assertThatCode(() -> validate(new Sample(null, true))).doesNotThrowAnyException();
        assertThatCode(() -> validate(new Sample(Profile.Type.MASTER, null))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Par nulo é válido")
    void nullPairIsValid() {
        assertThat(new MasterRequiresTwoFactor.PairValidator().isValid(null, null)).isTrue();
    }

    @MasterRequiresTwoFactor
    private record Sample(Profile.Type type, Boolean twoFactor) implements MasterRequiresTwoFactor.Pair {

    }

}
