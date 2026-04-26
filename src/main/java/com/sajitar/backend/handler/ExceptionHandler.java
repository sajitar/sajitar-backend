package com.sajitar.backend.handler;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.sajitar.backend.util.ResourceException;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class ExceptionHandler {

    @org.springframework.web.bind.annotation.ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> handle(final ConstraintViolationException exception) {
        final var allErrors = new HashMap<String, List<String>>();
        exception.getConstraintViolations().forEach(violation -> {
            final var propertyPath = violation.getPropertyPath().toString();
            final var propertyName = propertyPath.substring(propertyPath.lastIndexOf('.') + 1);
            allErrors.computeIfAbsent(propertyName, key -> {
                return new LinkedList<>();
            }).add(violation.getMessage());
        });
        return ResponseEntity.badRequest().body(allErrors);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handle(final MethodArgumentTypeMismatchException exception) {
        final var fullPropertyType = Optional.ofNullable(exception.getRequiredType()).map(Class::getName).orElse("unknown");
        final var propertyType = fullPropertyType.substring(fullPropertyType.lastIndexOf('.') + 1);
        final var propertyName = exception.getName();
        return ResponseEntity.badRequest().body(Map.of(propertyName, List.of("deve pertencer ao tipo ".concat(propertyType))));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(NullPointerException.class)
    public ResponseEntity<?> handle() {
        return ResponseEntity.internalServerError().build();
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(ResourceException.class)
    public ResponseEntity<?> handle(final ResourceException exception) {
        return exception.getContent().isEmpty()
                ? ResponseEntity.status(exception.getStatus()).build()
                : ResponseEntity.status(exception.getStatus()).body(exception.getContent());
    }

}
