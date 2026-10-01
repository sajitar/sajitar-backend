package com.sajitar.backend.domain.validation.profile;

import static jakarta.validation.Validation.buildDefaultValidatorFactory;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.sajitar.backend.domain.model.profile.Profile;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Payload;
import jakarta.validation.Validator;
import lombok.experimental.UtilityClass;

@Documented
@Constraint(validatedBy = MasterRequiresTwoFactor.PairValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MasterRequiresTwoFactor {

    String message() default "{validation.two-factor.master}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    interface Pair {

        Profile.Type type();

        Boolean twoFactor();

    }

    public final class PairValidator implements ConstraintValidator<MasterRequiresTwoFactor, Pair> {

        @Override
        public boolean isValid(final Pair value, final ConstraintValidatorContext context) {
            if (value == null) {
                return true;
            }
            final var type = value.type();
            final var twoFactor = value.twoFactor();
            if (type == null || twoFactor == null || !type.includes(Profile.Type.MASTER) || twoFactor) {
                return true;
            }
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("twoFactor")
                    .addConstraintViolation();
            return false;
        }

    }

    @UtilityClass
    final class Validation {

        public static void validate(final Profile.Type type, final boolean twoFactor) {
            validate(new Snapshot(type, twoFactor));
        }

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

        @MasterRequiresTwoFactor
        private record Snapshot(Profile.Type type, Boolean twoFactor) implements Pair {

        }

    }

}
