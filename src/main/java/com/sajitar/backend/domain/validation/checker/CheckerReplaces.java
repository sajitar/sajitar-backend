package com.sajitar.backend.domain.validation.checker;

import static jakarta.validation.Validation.buildDefaultValidatorFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Payload;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@NotNull(message = "{validation.not-null}")
@Min(value = CheckerReplaces.MIN, message = "{validation.checker.replaces.range}")
@Max(value = CheckerReplaces.MAX, message = "{validation.checker.replaces.range}")
@Constraint(validatedBy = {})
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckerReplaces {

    static final int MIN = 0;

    static final int MAX = 3;

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    @Builder(access = AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class Validation {
        private final @CheckerReplaces Integer replaces;

        public static Integer validate(final Integer target) {
            try (final var factory = buildDefaultValidatorFactory()) {
                return validate(factory.getValidator(), target);
            }
        }

        public static Integer validate(final Validator validator, final Integer target) {
            final var validations = validator.validate(builder().replaces(target).build());
            if (!validations.isEmpty()) {
                throw new ConstraintViolationException(validations);
            }
            return target;
        }

    }

}
