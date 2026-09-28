package com.sajitar.backend.domain.validation.profile;

import static com.sajitar.backend.domain.validation.profile.DifferentEmails.Validation.validate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolationException;

@DisplayName("Anotação @DifferentEmails")
class DifferentEmailsTest {

    @Test
    @DisplayName("Aceita e-mails distintos")
    void acceptsDistinctEmails() {
        final var sample = new Sample("user@example.com", "novo@example.com");

        assertThatCode(() -> validate(sample)).doesNotThrowAnyException();
        assertThat(validate(sample)).isEqualTo(sample);
    }

    @Test
    @DisplayName("Rejeita e-mail novo igual ao atual no campo newEmail")
    void rejectsEqualEmailsOnNewEmail() {
        final var thrown = catchThrowable(() -> validate(new Sample("user@example.com", "user@example.com")));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getPropertyPath()).hasToString("newEmail");
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType())
                .isEqualTo(DifferentEmails.class);
        assertThat(violation.getMessage()).isEqualTo("must differ from the current email");
    }

    @Test
    @DisplayName("Não compara quando algum e-mail é nulo")
    void skipsWhenEitherEmailIsNull() {
        assertThatCode(() -> validate(new Sample(null, "novo@example.com"))).doesNotThrowAnyException();
        assertThatCode(() -> validate(new Sample("user@example.com", null))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validate(Validator) rejeita e-mails iguais")
    void validateWithValidatorRejectsEqualEmails() {
        try (final var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            final var thrown = catchThrowable(() -> DifferentEmails.Validation.validate(
                    factory.getValidator(),
                    new Sample("user@example.com", "user@example.com")));

            assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        }
    }

    @Test
    @DisplayName("requireDifferent aceita e-mails distintos")
    void requireDifferentAcceptsDistinctEmails() {
        try (final var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            DifferentEmails.Validation.requireDifferent(
                    factory.getValidator(),
                    "user@example.com",
                    "novo@example.com");
        }
    }

    @Test
    @DisplayName("requireDifferent rejeita e-mails iguais")
    void requireDifferentRejectsEqualEmails() {
        try (final var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            final var thrown = catchThrowable(() -> DifferentEmails.Validation.requireDifferent(
                    factory.getValidator(),
                    "user@example.com",
                    "user@example.com"));

            assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        }
    }

    @Test
    @DisplayName("Par nulo é válido")
    void nullPairIsValid() {
        assertThat(new DifferentEmails.PairValidator().isValid(null, null)).isTrue();
    }

    @DifferentEmails
    private record Sample(String currentEmail, String newEmail) implements DifferentEmails.Pair {

    }

}
