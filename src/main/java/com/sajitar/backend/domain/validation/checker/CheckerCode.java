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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@NotBlank(message = "{validation.not-blank}")
@Pattern(regexp = "^[0-9]{6}$", message = "{validation.checker.code.pattern}")
@Constraint(validatedBy = {})
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckerCode {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    @Builder(access = AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class Validation {
        private final @CheckerCode String code;

        public static String validate(final String target) {
            try (final var factory = buildDefaultValidatorFactory()) {
                return validate(factory.getValidator(), target);
            }
        }

        public static String validate(final Validator validator, final String target) {
            final var validations = validator.validate(builder().code(target).build());
            if (!validations.isEmpty()) {
                throw new ConstraintViolationException(validations);
            }
            return target;
        }

    }

}
