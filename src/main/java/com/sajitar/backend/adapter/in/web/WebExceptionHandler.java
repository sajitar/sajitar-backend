package com.sajitar.backend.adapter.in.web;

import static org.springframework.http.HttpStatus.CONFLICT;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.sajitar.backend.domain.exception.DomainException;
import com.sajitar.backend.domain.exception.EmailAlreadyRegisteredException;
import com.sajitar.backend.domain.exception.ProfileNotFoundException;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class WebExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, List<String>>> handle(final ConstraintViolationException exception) {
        final var allErrors = new HashMap<String, List<String>>();
        exception.getConstraintViolations().forEach(violation -> {
            final var propertyPath = violation.getPropertyPath().toString();
            final var propertyName = propertyPath.substring(propertyPath.lastIndexOf('.') + 1);
            allErrors.computeIfAbsent(propertyName, _ -> new LinkedList<>()).add(violation.getMessage());
        });
        return ResponseEntity.badRequest().body(allErrors);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, List<String>>> handle(final MethodArgumentNotValidException exception) {
        final var allErrors = new HashMap<String, List<String>>();
        exception.getBindingResult().getFieldErrors().forEach(error -> {
            allErrors.computeIfAbsent(error.getField(), _ -> new LinkedList<>())
                    .add(error.getDefaultMessage());
        });
        return ResponseEntity.badRequest().body(allErrors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, List<String>>> handle(final MethodArgumentTypeMismatchException exception) {
        final var fullPropertyType = Optional.ofNullable(exception.getRequiredType()).map(Class::getName).orElse("unknown");
        final var propertyType = fullPropertyType.substring(fullPropertyType.lastIndexOf('.') + 1);
        final var propertyName = exception.getName();
        return ResponseEntity.badRequest().body(Map.of(propertyName, List.of("deve pertencer ao tipo ".concat(propertyType))));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<Map<String, List<String>>> handle(final DomainException exception) {
        return switch (exception) {
            case EmailAlreadyRegisteredException conflict -> ResponseEntity.status(CONFLICT).body(conflict.content());
            case ProfileNotFoundException _ -> ResponseEntity.notFound().build();
        };
    }

}
