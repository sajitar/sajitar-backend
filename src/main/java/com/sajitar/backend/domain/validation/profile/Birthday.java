package com.sajitar.backend.domain.validation.profile;

import static jakarta.validation.Validation.buildDefaultValidatorFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Objects;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Payload;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@NotNull(message = "{validation.not-null}")
@Constraint(validatedBy = Birthday.BirthdayValidator.class)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Birthday {

    String message() default "{validation.birthday.min-age}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    public class BirthdayValidator implements ConstraintValidator<Birthday, LocalDate> {

        protected static volatile int minAgeYears;

        public static void configure(final int minAge) {
            minAgeYears = minAge;
        }

        @Override
        public boolean isValid(final LocalDate birthday, final ConstraintValidatorContext context) {
            if (Objects.isNull(birthday)) {
                return true;
            }
            final var today = LocalDate.now(ZoneId.systemDefault());
            final var ageYears = Period.between(birthday, today).getYears();
            if (ageYears >= minAgeYears) {
                return true;
            }
            context.unwrap(HibernateConstraintValidatorContext.class).addMessageParameter("minAge", minAgeYears);
            return false;
        }

    }

    @Builder(access = AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class Validation {
        private final @Birthday LocalDate birthday;

        public static LocalDate validate(final LocalDate target) {
            try (final var factory = buildDefaultValidatorFactory()) {
                return validate(factory.getValidator(), target);
            }
        }

        public static LocalDate validate(final Validator validator, final LocalDate target) {
            final var validations = validator.validate(builder().birthday(target).build());
            if (!validations.isEmpty()) {
                throw new ConstraintViolationException(validations);
            }
            return target;
        }

    }

}
