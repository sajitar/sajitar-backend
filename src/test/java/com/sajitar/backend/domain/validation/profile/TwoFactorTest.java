package com.sajitar.backend.domain.validation.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.constraints.NotNull;

@DisplayName("Anotação @TwoFactor (perfil)")
class TwoFactorTest {

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    @DisplayName("Aceita verdadeiro e falso")
    void acceptsBoolean(final boolean value) {
        assertThat(TwoFactor.Validation.validate(value)).isEqualTo(value);
        assertThat(TwoFactor.Validation.validate(Validation.buildDefaultValidatorFactory().getValidator(), value))
                .isEqualTo(value);
    }

    @Test
    @DisplayName("Nulo responde 400")
    void rejectsNull() {
        final var thrown = catchThrowable(() -> TwoFactor.Validation.validate(null));

        assertThat(thrown).isInstanceOf(ConstraintViolationException.class);
        final var violation = ((ConstraintViolationException) thrown).getConstraintViolations().iterator().next();
        assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType()).isEqualTo(NotNull.class);
    }

}
