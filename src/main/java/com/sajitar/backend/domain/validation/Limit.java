package com.sajitar.backend.domain.validation;

import static jakarta.validation.Validation.buildDefaultValidatorFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;

import org.apache.commons.lang3.math.NumberUtils;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@NotNull
@Constraint(validatedBy = Limit.LimitValidator.class)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Limit {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    public class LimitValidator implements ConstraintValidator<Limit, Integer> {

        protected static volatile int max;

        private String resolvedMessage;

        @Override
        public void initialize(final Limit constraintAnnotation) {
            resolvedMessage = "deve ser um número positivo menor ou igual à " + max;
        }

        @Override
        public boolean isValid(final Integer limit, final ConstraintValidatorContext context) {
            if (Objects.isNull(limit)) {
                return true;
            }
            if (limit > NumberUtils.INTEGER_ZERO && limit <= max) {
                return true;
            }
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(resolvedMessage).addConstraintViolation();
            return false;
        }

    }

    @Builder(access = AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class Validation {
        private final @Limit Integer limit;

        public static Integer validate(final Integer target) {
            try (final var factory = buildDefaultValidatorFactory()) {
                final var validations = factory.getValidator().validate(builder().limit(target).build());
                if (!validations.isEmpty()) {
                    throw new ConstraintViolationException(validations);
                }
            }
            return target;
        }

    }

}
