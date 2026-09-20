package com.sajitar.backend.application;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Constraints {

    public static void requireValid(final Validator validator, final Object target) {
        final var violations = validator.validate(target);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

}
