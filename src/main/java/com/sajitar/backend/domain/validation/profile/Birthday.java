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
@Constraint(validatedBy = Birthday.BirthdayValidator.class)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Birthday {

    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    public class BirthdayValidator implements ConstraintValidator<Birthday, LocalDate> {

        protected static volatile int minAgeYears;

        private String resolvedMessage;

        @Override
        public void initialize(final Birthday constraintAnnotation) {
            resolvedMessage = "deve ter mais de " + minAgeYears + " anos";
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
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(resolvedMessage).addConstraintViolation();
            return false;
        }

    }

    @Builder(access = AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class Validation {
        private final @Birthday LocalDate birthday;

        public static LocalDate validate(final LocalDate target) {
            try (final var factory = buildDefaultValidatorFactory()) {
                final var validations = factory.getValidator().validate(builder().birthday(target).build());
                if (!validations.isEmpty()) {
                    throw new ConstraintViolationException(validations);
                }
            }
            return target;
        }

    }

}
