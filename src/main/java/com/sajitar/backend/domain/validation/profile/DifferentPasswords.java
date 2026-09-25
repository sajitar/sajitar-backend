package com.sajitar.backend.domain.validation.profile;

import static jakarta.validation.Validation.buildDefaultValidatorFactory;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Payload;
import jakarta.validation.Validator;
import lombok.experimental.UtilityClass;

@Documented
@Constraint(validatedBy = DifferentPasswords.PairValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DifferentPasswords {

    String message() default "{validation.password.must-differ}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    interface Pair {

        String currentPassword();

        String newPassword();

    }

    public final class PairValidator implements ConstraintValidator<DifferentPasswords, Pair> {

        @Override
        public boolean isValid(final Pair value, final ConstraintValidatorContext context) {
            if (value == null) {
                return true;
            }
            final var current = value.currentPassword();
            final var replacement = value.newPassword();
            if (current == null || replacement == null || !current.equals(replacement)) {
                return true;
            }
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("newPassword")
                    .addConstraintViolation();
            return false;
        }

    }

    @UtilityClass
    final class Validation {

        public static <T extends Pair> T validate(final T target) {
            try (final var factory = buildDefaultValidatorFactory()) {
                return validate(factory.getValidator(), target);
            }
        }

        public static <T extends Pair> T validate(final Validator validator, final T target) {
            final var validations = validator.validate(target);
            if (!validations.isEmpty()) {
                throw new ConstraintViolationException(validations);
            }
            return target;
        }

    }

}
