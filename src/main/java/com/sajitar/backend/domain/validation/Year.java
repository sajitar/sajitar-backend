package com.sajitar.backend.domain.validation;

import static jakarta.validation.Validation.buildDefaultValidatorFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Objects;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Payload;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@Constraint(validatedBy = Year.YearValidation.class)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Year {

    long min() default Long.MIN_VALUE;

    long max() default Long.MAX_VALUE;

    boolean nullable() default true;

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class YearValidation implements ConstraintValidator<Year, LocalDate> {

        private Year year;

        @Override
        public void initialize(final Year year) {
            this.year = year;
        }

        @Override
        public boolean isValid(final LocalDate targetDate, final ConstraintValidatorContext context) {
            if (Objects.isNull(targetDate)) {
                return year.nullable();
            }
            final var currentDate = LocalDate.now(ZoneId.systemDefault());
            final var years = Period.between(targetDate, currentDate).getYears();
            return years >= year.min() && years <= year.max();
        }

    }

    @Builder(access = AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class Validation {
        private final @Year(min = 0, max = 150, nullable = true) LocalDate year;

        public static LocalDate validate(final LocalDate target) {
            try (final var factory = buildDefaultValidatorFactory()) {
                final var validations = factory.getValidator().validate(builder().year(target).build());
                if (!validations.isEmpty()) {
                    throw new ConstraintViolationException(validations);
                }
            }
            return target;
        }

    }

}