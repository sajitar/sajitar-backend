package com.sajitar.backend.domain.validation.profile;

import static com.sajitar.backend.domain.validation.profile.DifferentPasswords.Validation.validate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolationException;

@DisplayName("Anotação @DifferentPasswords")
class DifferentPasswordsTest {

    @Test
    @DisplayName("Aceita senhas distintas")
    void acceptsDistinctPasswords() {
        final var sample = new Sample("senhaAtual12", "senhaNovaSegura1");

        assertThatCode(() -> validate(sample)).doesNotThrowAnyException();
        assertThat(validate(sample)).isEqualTo(sample);
    }

    @Test
    @DisplayName("Rejeita senha nova igual à atual no campo newPassword")
    void rejectsEqualPasswordsOnNewPassword() {
        final var thrown = catchThrowable(() -> validate(new Sample("senhaAtual12", "senhaAtual12")));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getPropertyPath()).hasToString("newPassword");
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType())
                .isEqualTo(DifferentPasswords.class);
        assertThat(violation.getMessage()).isEqualTo("must differ from the current password");
    }

    @Test
    @DisplayName("Não compara quando alguma senha é nula")
    void skipsWhenEitherPasswordIsNull() {
        assertThatCode(() -> validate(new Sample(null, "senhaNovaSegura1"))).doesNotThrowAnyException();
        assertThatCode(() -> validate(new Sample("senhaAtual12", null))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validate(Validator) rejeita senhas iguais")
    void validateWithValidatorRejectsEqualPasswords() {
        try (final var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            final var thrown = catchThrowable(() -> DifferentPasswords.Validation.validate(
                    factory.getValidator(),
                    new Sample("senhaAtual12", "senhaAtual12")));

            assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        }
    }

    @Test
    @DisplayName("Par nulo é válido")
    void nullPairIsValid() {
        assertThat(new DifferentPasswords.PairValidator().isValid(null, null)).isTrue();
    }

    @DifferentPasswords
    private record Sample(String currentPassword, String newPassword) implements DifferentPasswords.Pair {

    }

}
